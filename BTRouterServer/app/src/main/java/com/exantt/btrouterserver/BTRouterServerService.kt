package com.exantt.btrouterserver

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class BTRouterServerService : Service() {

    companion object {
        private const val TAG = "BT_SERVER_SVC"
        private const val APP_NAME = "BTRouterServer"

        const val STATUS_CHANNEL_ID = "ServerStatusChannel"
        const val HOTSPOT_TRIGGER_CHANNEL_ID = "HotspotTriggerChannel"
        const val MOBILE_DATA_TRIGGER_CHANNEL_ID = "MobileDataTriggerChannel"
        const val STATUS_NOTIFICATION_ID = 2001

        private const val ACTION_SMS_SENT = "com.exantt.btrouterserver.ACTION_SMS_SENT"
        private const val EXTRA_SMS_CMD_ID = "cmd_id"
        private const val EXTRA_SMS_PART_INDEX = "part_index"

        private const val HOTSPOT_TRIGGER_ON_KEYWORD = "BTR_HOTSPOT_ON"
        private const val HOTSPOT_TRIGGER_OFF_KEYWORD = "BTR_HOTSPOT_OFF"
        private const val MOBILE_DATA_TRIGGER_ON_KEYWORD = "BTR_MOBILE_DATA_ON"
        private const val MOBILE_DATA_TRIGGER_OFF_KEYWORD = "BTR_MOBILE_DATA_OFF"
        private const val ROUTINE_TRIGGER_CLEANUP_MS = 3_500L
        private const val ACTION_WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val EXTRA_WIFI_AP_STATE = "wifi_state"
        private const val WIFI_AP_STATE_DISABLED = 11
        private const val WIFI_AP_STATE_ENABLED = 13
        private const val ACTION_TETHER_STATE_CHANGED = "android.net.conn.TETHER_STATE_CHANGED"
        private const val EXTRA_ACTIVE_TETHER = "tetherArray"
        private const val STATUS_POLL_ACTIVE_MS = 3_000L
        private const val STATUS_POLL_IDLE_MS = 30_000L
        private const val HOTSPOT_RECHECK_FIRST_MS = 1_500L
        private const val HOTSPOT_RECHECK_SECOND_MS = 3_500L

        private const val PREFS_NAME = "bt_router_server_prefs"
        private const val KEY_SHARED_SECRET = "shared_secret"
        private const val KEY_ALLOWED_MAC = "allowed_client_mac"
        private const val KEY_ALLOWED_NAME = "allowed_client_name"
        private const val KEY_LAST_DISCONNECT_TIME = "last_disconnect_time"
        private const val KEY_LAST_SYNCED_SMS_ID = "last_synced_sms_id"
        private const val KEY_LAST_SYNCED_CALL_ID = "last_synced_call_id"
        private const val KEY_UNACKED_LIVE_SMS = "unacked_live_sms"

        private const val HANDSHAKE_TIMEOUT_SEC = 10L
        private const val CLIENT_IDLE_TIMEOUT_MS = 180_000L
        private const val MAX_SYNC_BATCH = 50
        private const val MAX_SMS_PARTS = 10
        private const val SMS_MODEM_TIMEOUT_MS = 25_000L
        private const val SMS_INTER_SEND_DELAY_MS = 1_200L
        private const val MAX_UNACKED_LIVE_SMS = 30
        private const val UNACKED_TTL_MS = 24 * 60 * 60_000L
        private const val MAX_EXPORT_CONTACTS = 5000
        private const val CONTACTS_CHUNK_MAX_ITEMS = 150
        private const val CONTACTS_CHUNK_MAX_BYTES = 32_000

        @Synchronized
        fun getOrCreatePairingSecret(context: Context): String {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val current = SecretStorage.loadSecret(sp, KEY_SHARED_SECRET)
            if (!current.isNullOrBlank() && BtProtocol.isValidSecret(current)) {
                return current
            }
            val generated = BtProtocol.generatePairingSecret()
            SecretStorage.saveSecret(sp, KEY_SHARED_SECRET, generated)
            return generated
        }

        @Synchronized
        fun regeneratePairingSecret(context: Context): String {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fresh = BtProtocol.generatePairingSecret()
            SecretStorage.saveSecret(sp, KEY_SHARED_SECRET, fresh)
            return fresh
        }

        fun getAllowedClientMac(context: Context): String? =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ALLOWED_MAC, null)

        fun getAllowedClientName(context: Context): String? =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_ALLOWED_NAME, null)

        fun setAllowedClient(context: Context, mac: String, name: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ALLOWED_MAC, mac.trim())
                .putString(KEY_ALLOWED_NAME, name.trim())
                .apply()
        }
    }

    interface ServiceCallback {
        fun onServiceStateChanged(stateText: String, isConnected: Boolean, connectedClientName: String)
    }

    inner class LocalBinder : Binder() {
        fun getService(): BTRouterServerService = this@BTRouterServerService
    }

    private data class LiveSmsRecord(
        val eventId: String,
        val sender: String,
        val body: String,
        val receivedAtMs: Long
    )

    private data class PendingServerEvent(
        val eventId: String,
        val kind: String, // "LIVE_SMS", "SMS_SYNC", "CALL_SYNC"
        val dbId: Long,
        val eventTs: Long,
        val payloadJson: String
    )

    private val binder = LocalBinder()
    @Volatile
    var callback: ServiceCallback? = null

    private lateinit var prefs: SharedPreferences
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeLockExpiresAtMs = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sendExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val syncExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val watchdogExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val serverLoopLock = Object()
    private val statusSyncLock = Any()
    private val liveSmsLock = Any()
    private val smsTrackerLock = Any()
    private val unackedQueueLock = Any()
    private val syncAckLock = Any()

    @Volatile
    private var isInitialized = false
    @Volatile
    private var isServerRunning = true
    @Volatile
    private var activeServerSocket: BluetoothServerSocket? = null
    @Volatile
    private var activeSocket: BluetoothSocket? = null
    @Volatile
    private var activeChannel: BtProtocol.SecureChannel? = null

    @Volatile
    var isConnected: Boolean = false
        private set
    @Volatile
    var currentStateText: String = "Инициализация сервера..."
        private set
    @Volatile
    var connectedClientLabel: String = ""
        private set

    @Volatile
    private var lastPostedNotifText: String = ""
    @Volatile
    private var lastSentStatusJson: String = ""
    @Volatile
    private var lastStatusSentAtMs: Long = 0L
    @Volatile
    private var lastClientActivityMs: Long = 0L

    @Volatile
    private var lastDisconnectTime: Long = 0L
    @Volatile
    private var lastSyncedSmsId: Long = 0L
    @Volatile
    private var lastSyncedCallId: Long = 0L
    @Volatile
    private var sessionConnectedAt: Long = 0L

    private var pendingDisconnectCollecting = false
    private val pendingDisconnectEventIds = HashSet<String>()
    private var pendingDisconnectTargetTime = 0L
    private var pendingDisconnectHadMore = false

    @Volatile
    private var isIncomingCallRinging: Boolean = false
    @Volatile
    private var lastIncomingNumber: String = ""
    @Volatile
    private var lastIncomingName: String = ""
    @Volatile
    private var hiddenCallSentForSession: Boolean = false
    private var hiddenCallRunnable: Runnable? = null

    private var unauthorizedCount = 0
    private var lastUnauthorizedAtMs = 0L

    private class RoutineTriggerState(
        val notifIdGen: AtomicInteger
    ) {
        val lock = Any()
        var lastNotifId: Int = -1
        var cancelRunnable: Runnable? = null
    }

    private val smsRequestCodeGen = AtomicInteger(5000)
    private val hotspotTriggerState = RoutineTriggerState(AtomicInteger(2100))
    private val mobileDataTriggerState = RoutineTriggerState(AtomicInteger(3100))
    private val hotspotStateLock = Any()
    private val mobileDataStateLock = Any()
    @Volatile
    private var hotspotState: Int = -1
    @Volatile
    private var mobileDataState: Int = -1
    private var mobileDataObserver: ContentObserver? = null
    private val outboundSmsQueue = OutboundSmsQueue()
    private var activeSmsTimeoutFuture: ScheduledFuture<*>? = null
    private var nextSmsDispatchFuture: ScheduledFuture<*>? = null
    private val liveDeliveredSms = ArrayDeque<LiveSmsRecord>(50)
    private val pendingEventAcks = ConcurrentHashMap<String, PendingServerEvent>()
    private val unackedLiveSms = LinkedHashMap<String, PendingServerEvent>()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Безопасный выход без ForegroundServiceDidNotStartInTimeException / SecurityException
        if (!CompatUtils.hasBluetoothConnectPermission(this)) {
            Log.w(TAG, "Нет разрешения BLUETOOTH_CONNECT — остановка службы")
            try {
                val fallbackNotif = buildNotification("Требуется разрешение Bluetooth")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(STATUS_NOTIFICATION_ID, fallbackNotif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
                } else {
                    startForeground(STATUS_NOTIFICATION_ID, fallbackNotif)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {
            }
            stopSelf()
            return
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        ensurePairingSecretExists()
        lastDisconnectTime = prefs.getLong(KEY_LAST_DISCONNECT_TIME, 0L)
        lastSyncedSmsId = prefs.getLong(KEY_LAST_SYNCED_SMS_ID, 0L)
        lastSyncedCallId = prefs.getLong(KEY_LAST_SYNCED_CALL_ID, 0L)
        loadUnackedLiveSms()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BTRouterServer:ServiceWakeLock").apply {
            setReferenceCounted(false)
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val initialText = if (getAllowedClientMac().isNullOrBlank()) {
            "Client не выбран (подключения отклоняются)"
        } else {
            "Ожидание подключения Client..."
        }
        currentStateText = initialText
        lastPostedNotifText = initialText
        val initialNotif = buildNotification(initialText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(STATUS_NOTIFICATION_ID, initialNotif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(STATUS_NOTIFICATION_ID, initialNotif)
        }

        ContextCompat.registerReceiver(
            this,
            callReceiver,
            IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            smsReceiver,
            IntentFilter("android.provider.Telephony.SMS_RECEIVED"),
            ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            btStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            smsSentReceiver,
            IntentFilter(ACTION_SMS_SENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val stickyApIntent = ContextCompat.registerReceiver(
            this,
            wifiApStateReceiver,
            IntentFilter(ACTION_WIFI_AP_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        updateHotspotStateFromApInt(
            stickyApIntent?.getIntExtra(EXTRA_WIFI_AP_STATE, -1) ?: -1,
            pushStatus = false
        )
        val stickyTetherIntent = ContextCompat.registerReceiver(
            this,
            tetherStateReceiver,
            IntentFilter(ACTION_TETHER_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )
        val stickyTetherList = try {
            stickyTetherIntent?.getStringArrayListExtra(EXTRA_ACTIVE_TETHER)
        } catch (_: Throwable) {
            null
        }
        val stickyTetherHint = HotspotDetector.detectFromTetherArray(stickyTetherList)
        if (stickyTetherHint == 0 || stickyTetherHint == 1) {
            applyHotspotState(stickyTetherHint, pushStatus = false)
        }
        if (!syncExecutor.isShutdown) {
            syncExecutor.execute {
                if (isServerRunning) {
                    refreshHotspotState(pushStatus = false, tetherHint = stickyTetherHint)
                }
            }
        }
        refreshMobileDataState(pushStatus = false)
        try {
            val observer = object : ContentObserver(mainHandler) {
                override fun onChange(selfChange: Boolean) {
                    if (isServerRunning) {
                        refreshMobileDataState(pushStatus = true)
                    }
                }
            }
            contentResolver.registerContentObserver(
                Settings.Global.getUriFor("mobile_data"),
                false,
                observer
            )
            mobileDataObserver = observer
        } catch (_: Exception) {
        }

        isInitialized = true
        thread(name = "BTServerLoop") { startBluetoothServer() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isInitialized) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    fun getPairingSecret(): String = getOrCreatePairingSecret(this)

    fun regeneratePairingSecret(): String {
        val fresh = regeneratePairingSecret(this)
        onConfigUpdated()
        return fresh
    }

    fun getAllowedClientMac(): String? = getAllowedClientMac(this)

    fun getAllowedClientName(): String? = getAllowedClientName(this)

    fun setAllowedClient(mac: String, name: String) {
        setAllowedClient(this, mac, name)
        onConfigUpdated()
    }

    fun onConfigUpdated() {
        closeActiveClientSession()
        val msg = if (getAllowedClientMac().isNullOrBlank()) {
            "Client не выбран (подключения отклоняются)"
        } else {
            "Разрешён Client — ожидание подключения..."
        }
        updateState(msg, false, "")
        wakeServerLoop()
    }

    fun sendTestCall() {
        if (!BuildConfig.DEBUG) return
        val json = JSONObject().apply {
            put("t", "CALL_RINGING")
            put("number", "+90 555 0000000")
            put("name", "Тестовый номер")
        }
        sendJson(json)
    }

    fun sendTestMissedCall() {
        if (!BuildConfig.DEBUG) return
        val ts = System.currentTimeMillis()
        val json = JSONObject().apply {
            put("t", "MISSED_CALL")
            put("eventId", "test_missed_$ts")
            put("number", "+90 555 0000000")
            put("name", "Тестовый номер")
            put("ts", ts)
        }
        sendJson(json)
    }

    fun sendStatusNow() {
        if (!BuildConfig.DEBUG) return
        sendStatusIfChanged(force = true)
    }

    private fun ensurePairingSecretExists() {
        getOrCreatePairingSecret(this)
    }

    private fun loadUnackedLiveSms() {
        val raw = prefs.getString(KEY_UNACKED_LIVE_SMS, null) ?: return
        val now = System.currentTimeMillis()
        var dirty = false
        synchronized(unackedQueueLock) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val eventId = obj.optString("eventId", "")
                    val eventTs = obj.optLong("eventTs", 0L)
                    val payload = obj.optString("payloadJson", "")
                    if (eventId.isEmpty() || payload.isEmpty() || (now - eventTs > UNACKED_TTL_MS)) {
                        dirty = true
                        continue
                    }
                    val ev = PendingServerEvent(
                        eventId = eventId,
                        kind = "LIVE_SMS",
                        dbId = 0L,
                        eventTs = eventTs,
                        payloadJson = payload
                    )
                    unackedLiveSms[eventId] = ev
                    pendingEventAcks[eventId] = ev
                }
            } catch (_: Exception) {
                unackedLiveSms.clear()
                dirty = true
            }
            if (dirty) {
                persistUnackedLiveSmsLocked()
            }
        }
    }

    private fun persistUnackedLiveSmsLocked() {
        if (!this::prefs.isInitialized) return
        val arr = JSONArray()
        for (ev in unackedLiveSms.values) {
            arr.put(JSONObject().apply {
                put("eventId", ev.eventId)
                put("eventTs", ev.eventTs)
                put("payloadJson", ev.payloadJson)
            })
        }
        prefs.edit().putString(KEY_UNACKED_LIVE_SMS, arr.toString()).apply()
    }

    private fun enqueueUnackedLiveSms(ev: PendingServerEvent) {
        val now = System.currentTimeMillis()
        synchronized(unackedQueueLock) {
            val iter = unackedLiveSms.entries.iterator()
            while (iter.hasNext()) {
                if (now - iter.next().value.eventTs > UNACKED_TTL_MS) {
                    iter.remove()
                }
            }
            while (unackedLiveSms.size >= MAX_UNACKED_LIVE_SMS) {
                val oldest = unackedLiveSms.keys.firstOrNull() ?: break
                unackedLiveSms.remove(oldest)
                pendingEventAcks.remove(oldest)
            }
            unackedLiveSms[ev.eventId] = ev
            pendingEventAcks[ev.eventId] = ev
            persistUnackedLiveSmsLocked()
        }
    }

    private fun flushUnackedLiveSms(targetChannel: BtProtocol.SecureChannel) {
        val now = System.currentTimeMillis()
        val toResend: List<PendingServerEvent>
        synchronized(unackedQueueLock) {
            var removedExpired = false
            val iter = unackedLiveSms.entries.iterator()
            while (iter.hasNext()) {
                if (now - iter.next().value.eventTs > UNACKED_TTL_MS) {
                    iter.remove()
                    removedExpired = true
                }
            }
            if (removedExpired) {
                persistUnackedLiveSmsLocked()
            }
            toResend = unackedLiveSms.values.toList()
        }
        for (ev in toResend) {
            pendingEventAcks[ev.eventId] = ev
            sendRawPayload(ev.payloadJson, targetChannel)
        }
    }

    @Synchronized
    private fun acquirePulseWakeLock(timeoutMs: Long) {
        try {
            val now = System.currentTimeMillis()
            val desiredExpiry = now + timeoutMs
            if (desiredExpiry > wakeLockExpiresAtMs) {
                wakeLockExpiresAtMs = desiredExpiry
                wakeLock?.acquire(timeoutMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка захвата WakeLock")
        }
    }

    @Synchronized
    private fun releasePulseWakeLock() {
        try {
            wakeLockExpiresAtMs = 0L
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка освобождения WakeLock")
        }
    }

    private fun wakeServerLoop() {
        synchronized(serverLoopLock) {
            serverLoopLock.notifyAll()
        }
    }

    private fun waitInServerLoop(delayMs: Long) {
        synchronized(serverLoopLock) {
            if (!isServerRunning) return
            try {
                serverLoopLock.wait(delayMs)
            } catch (_: InterruptedException) {
            }
        }
    }

    private fun closeActiveClientSession() {
        try { activeSocket?.close() } catch (_: Exception) {}
        activeSocket = null
        activeChannel = null
        isConnected = false
    }

    private fun closeServerSocket() {
        try { activeServerSocket?.close() } catch (_: Exception) {}
        activeServerSocket = null
    }

    private fun recordDisconnectTime() {
        val now = System.currentTimeMillis()
        lastDisconnectTime = now
        if (this::prefs.isInitialized) {
            prefs.edit().putLong(KEY_LAST_DISCONNECT_TIME, now).apply()
        }
    }

    private fun startBluetoothServer() {
        while (isServerRunning) {
            if (!CompatUtils.hasBluetoothConnectPermission(this)) {
                updateState("Нет разрешения BLUETOOTH_CONNECT", false, "")
                waitInServerLoop(5_000L)
                continue
            }

            val adapter = bluetoothAdapter
            if (adapter == null || !adapter.isEnabled) {
                updateState("Bluetooth на Server выключен", false, "")
                waitInServerLoop(5 * 60_000L)
                continue
            }

            var serverSocket: BluetoothServerSocket? = null
            try {
                Log.d(TAG, "Открытие RFCOMM-сервера с выделенным UUID...")
                serverSocket = try {
                    adapter.listenUsingRfcommWithServiceRecord(APP_NAME, BtProtocol.APP_UUID)
                } catch (secEx: SecurityException) {
                    updateState("Отказано в праве Bluetooth", false, "")
                    waitInServerLoop(5_000L)
                    continue
                }
                activeServerSocket = serverSocket

                while (isServerRunning && bluetoothAdapter?.isEnabled == true) {
                    val waitMessage = if (getAllowedClientMac().isNullOrBlank()) {
                        "Client не выбран (подключения отклоняются)"
                    } else {
                        "Ожидание авторизованного Client..."
                    }
                    updateState(waitMessage, false, "")

                    val socket = serverSocket?.accept() ?: break
                    acquirePulseWakeLock(12_000L)

                    val expectedMac = getAllowedClientMac()
                    val remoteMac = try {
                        socket.remoteDevice?.address ?: ""
                    } catch (_: SecurityException) {
                        ""
                    }

                    // Fail-closed + backoff при спаме чужими спаренными устройствами
                    if (expectedMac.isNullOrBlank() || !remoteMac.equals(expectedMac, ignoreCase = true)) {
                        Log.w(TAG, "Отклонено подключение от неавторизованного устройства")
                        try { socket.close() } catch (_: Exception) {}
                        val now = System.currentTimeMillis()
                        unauthorizedCount = if (now - lastUnauthorizedAtMs > 60_000L) 1 else unauthorizedCount + 1
                        lastUnauthorizedAtMs = now
                        waitInServerLoop(minOf(500L * unauthorizedCount, 5_000L))
                        continue
                    }

                    unauthorizedCount = 0

                    val handshakeWatchdog = watchdogExecutor.schedule({
                        Log.w(TAG, "Таймаут рукопожатия Т1 — закрываю сокет")
                        try { socket.close() } catch (_: Exception) {}
                    }, HANDSHAKE_TIMEOUT_SEC, TimeUnit.SECONDS)

                    val channel: BtProtocol.SecureChannel
                    try {
                        val input = DataInputStream(BufferedInputStream(socket.inputStream))
                        val output = DataOutputStream(BufferedOutputStream(socket.outputStream))
                        val masterKey = BtProtocol.deriveMasterKey(getPairingSecret())
                        channel = BtProtocol.performServerHandshake(input, output, masterKey)
                    } catch (authEx: Exception) {
                        handshakeWatchdog.cancel(false)
                        Log.w(TAG, "Ошибка авторизации клиента")
                        try { socket.close() } catch (_: Exception) {}
                        updateState("Ошибка авторизации Client (проверьте ключ)", false, "")
                        waitInServerLoop(1_500L)
                        continue
                    } finally {
                        handshakeWatchdog.cancel(false)
                    }

                    activeSocket = socket
                    activeChannel = channel
                    sessionConnectedAt = System.currentTimeMillis()
                    lastClientActivityMs = sessionConnectedAt

                    val clientLabel = getAllowedClientName()?.ifBlank { null } ?: "Client"
                    updateState("Подключено и зашифровано: $clientLabel", true, clientLabel)

                    synchronized(statusSyncLock) {
                        lastSentStatusJson = ""
                    }
                    sendStatusIfChanged(force = true, targetChannel = channel)
                    flushUnackedLiveSms(channel)
                    startStatusReporter(socket, channel)

                    val idleWatchdog = watchdogExecutor.scheduleAtFixedRate({
                        if (activeSocket === socket && System.currentTimeMillis() - lastClientActivityMs > CLIENT_IDLE_TIMEOUT_MS) {
                            Log.w(TAG, "Таймаут активности Т1 (>3 мин) — закрываю сокет")
                            try { socket.close() } catch (_: Exception) {}
                        }
                    }, 60, 60, TimeUnit.SECONDS)

                    try {
                        while (isServerRunning && activeSocket === socket) {
                            val frameText = channel.receiveText()
                            lastClientActivityMs = System.currentTimeMillis()
                            acquirePulseWakeLock(4_000L)
                            val json = JSONObject(frameText)
                            handleClientCommand(json)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Сессия с Т1 завершена")
                        if (isServerRunning) {
                            recordDisconnectTime()
                        }
                    } finally {
                        idleWatchdog.cancel(false)
                        try { socket.close() } catch (_: Exception) {}
                        if (activeSocket === socket) {
                            activeSocket = null
                            activeChannel = null
                        }
                        isConnected = false
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Сбой серверного сокета (перезапуск)")
                if (isServerRunning) {
                    recordDisconnectTime()
                }
            } finally {
                closeServerSocket()
                try { serverSocket?.close() } catch (_: Exception) {}
            }

            if (isServerRunning) {
                waitInServerLoop(2_000L)
            }
        }
    }

    private fun handleClientCommand(json: JSONObject) {
        val type = json.optString("t", "")
        val cmdId = json.optString("id", "")

        when (type) {
            "PING" -> {
                sendJson(JSONObject().apply {
                    put("t", "PONG")
                    put("ts", json.optLong("ts", 0L))
                })
            }

            "EVENT_ACK" -> {
                val eventId = json.optString("eventId", "")
                if (eventId.isNotEmpty()) {
                    handleEventAck(eventId)
                }
            }

            "DECLINE" -> {
                val ringingNow = isIncomingCallRinging && isTelephonyRinging()
                if (!ringingNow) {
                    sendAck(cmdId, "DECLINE", false, "Входящий звонок уже не активен")
                    if (isIncomingCallRinging) {
                        isIncomingCallRinging = false
                        lastIncomingNumber = ""
                        lastIncomingName = ""
                    }
                    sendJson(JSONObject().put("t", "CALL_ENDED"))
                    return
                }

                val dropped = dropIncomingCall()
                if (dropped) {
                    isIncomingCallRinging = false
                    lastIncomingNumber = ""
                    lastIncomingName = ""
                    sendAck(cmdId, "DECLINE", true, "Входящий вызов сброшен")
                    sendJson(JSONObject().put("t", "CALL_ENDED"))
                } else {
                    sendAck(cmdId, "DECLINE", false, "Не удалось сбросить вызов (проверьте права)")
                }
            }

            "DIAL" -> {
                val number = json.optString("number", "")
                if (!BtProtocol.isStrictValidPhone(number)) {
                    sendAck(cmdId, "DIAL", false, "Некорректный формат номера")
                    return
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    sendAck(cmdId, "DIAL", false, "На Server нет разрешения CALL_PHONE")
                    return
                }
                try {
                    val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    val uri = Uri.fromParts("tel", number, null)
                    telecomManager.placeCall(uri, null)
                    sendAck(cmdId, "DIAL", true, "Звонок запущен на Server")
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка запуска исходящего вызова")
                    sendAck(cmdId, "DIAL", false, "Ошибка запуска звонка на Server")
                }
            }

            "SEND_SMS" -> {
                val number = json.optString("number", "")
                val body = json.optString("body", "")
                if (!BtProtocol.isStrictValidPhone(number) || body.isEmpty()) {
                    sendAck(cmdId, "SEND_SMS", false, "Некорректный номер или пустое сообщение")
                    return
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    sendAck(cmdId, "SEND_SMS", false, "На Server нет разрешения SEND_SMS")
                    return
                }
                val smsManager = CompatUtils.getSmsManager(this)
                if (smsManager == null) {
                    sendAck(cmdId, "SEND_SMS", false, "SMS-модем недоступен на Server")
                    return
                }

                try {
                    val parts = smsManager.divideMessage(body)
                    if (parts.isNullOrEmpty()) {
                        sendAck(cmdId, "SEND_SMS", false, "Не удалось подготовить части СМС")
                        return
                    }
                    if (parts.size > MAX_SMS_PARTS) {
                        sendAck(cmdId, "SEND_SMS", false, "Слишком длинное СМС (макс. $MAX_SMS_PARTS частей, получено ${parts.size})")
                        return
                    }

                    val item = OutboundSmsQueue.OutboundSmsItem(cmdId, number, ArrayList(parts))
                    val enqueueResult = synchronized(smsTrackerLock) {
                        outboundSmsQueue.enqueue(item)
                    }
                    if (!enqueueResult.accepted) {
                        sendAck(cmdId, "SEND_SMS", false, "Очередь отправки СМС на Server переполнена")
                        return
                    }

                    sendAck(cmdId, "SEND_SMS", true, "Передано в модем Server")
                    if (enqueueResult.dispatchImmediately) {
                        syncExecutor.execute { dispatchOutboundSms(item) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка подготовки СМС на сервере")
                    sendAck(cmdId, "SEND_SMS", false, "Ошибка модема Server при отправке СМС")
                }
            }

            "SYNC_5MIN" -> {
                syncExecutor.execute { syncRecentSms(cmdId) }
            }

            "SYNC_DISCONNECT" -> {
                syncExecutor.execute { syncAllSinceDisconnect(cmdId) }
            }

            "GET_STATUS" -> {
                sendStatusIfChanged(force = true)
            }

            "GET_CONTACTS" -> {
                syncExecutor.execute { sendContactsList(cmdId) }
            }

            "SET_HOTSPOT" -> {
                triggerHotspotRoutine(cmdId, json.optBoolean("enabled", false))
            }

            "SET_MOBILE_DATA" -> {
                triggerMobileDataRoutine(cmdId, json.optBoolean("enabled", false))
            }
        }
    }

    private fun handleEventAck(eventId: String) {
        val event = pendingEventAcks.remove(eventId)
        if (event != null) {
            when (event.kind) {
                "LIVE_SMS" -> {
                    synchronized(unackedQueueLock) {
                        if (unackedLiveSms.remove(eventId) != null) {
                            persistUnackedLiveSmsLocked()
                        }
                    }
                }
                "SMS_SYNC" -> {
                    synchronized(syncAckLock) {
                        if (event.dbId > lastSyncedSmsId) {
                            lastSyncedSmsId = event.dbId
                            if (this::prefs.isInitialized) {
                                prefs.edit().putLong(KEY_LAST_SYNCED_SMS_ID, lastSyncedSmsId).apply()
                            }
                        }
                    }
                }
                "CALL_SYNC" -> {
                    synchronized(syncAckLock) {
                        if (event.dbId > lastSyncedCallId) {
                            lastSyncedCallId = event.dbId
                            if (this::prefs.isInitialized) {
                                prefs.edit().putLong(KEY_LAST_SYNCED_CALL_ID, lastSyncedCallId).apply()
                            }
                        }
                    }
                }
            }
        } else {
            // Страховочная очистка живого SMS, если оно не попало в pendingEventAcks
            synchronized(unackedQueueLock) {
                if (unackedLiveSms.remove(eventId) != null) {
                    persistUnackedLiveSmsLocked()
                }
            }
        }

        synchronized(syncAckLock) {
            if (pendingDisconnectEventIds.remove(eventId)) {
                if (!pendingDisconnectCollecting && pendingDisconnectEventIds.isEmpty() && !pendingDisconnectHadMore && pendingDisconnectTargetTime > 0L) {
                    lastDisconnectTime = pendingDisconnectTargetTime
                    if (this::prefs.isInitialized) {
                        prefs.edit().putLong(KEY_LAST_DISCONNECT_TIME, lastDisconnectTime).apply()
                    }
                    pendingDisconnectTargetTime = 0L
                }
            }
        }
    }

    private fun sendAck(cmdId: String, cmd: String, ok: Boolean, msg: String) {
        if (cmdId.isEmpty()) return
        val ack = JSONObject().apply {
            put("t", "CMD_ACK")
            put("id", cmdId)
            put("cmd", cmd)
            put("ok", ok)
            put("msg", msg)
        }
        sendJson(ack)
    }

    private fun dispatchOutboundSms(item: OutboundSmsQueue.OutboundSmsItem) {
        acquirePulseWakeLock(SMS_MODEM_TIMEOUT_MS + 2_000L)
        val smsManager = CompatUtils.getSmsManager(this)
        if (smsManager == null) {
            failActiveOutboundSms(item.cmdId, "SMS-модем недоступен на Server")
            return
        }

        val sentIntents = ArrayList<PendingIntent>(item.parts.size)
        for (i in item.parts.indices) {
            val sentIntent = Intent(ACTION_SMS_SENT).apply {
                setPackage(packageName)
                putExtra(EXTRA_SMS_CMD_ID, item.cmdId)
                putExtra(EXTRA_SMS_PART_INDEX, i)
            }
            val pi = PendingIntent.getBroadcast(
                this,
                smsRequestCodeGen.getAndIncrement(),
                sentIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
            sentIntents.add(pi)
        }

        synchronized(smsTrackerLock) {
            activeSmsTimeoutFuture?.cancel(false)
            activeSmsTimeoutFuture = watchdogExecutor.schedule(
                { failActiveOutboundSms(item.cmdId, OutboundSmsQueue.MSG_MODEM_TIMEOUT) },
                SMS_MODEM_TIMEOUT_MS,
                TimeUnit.MILLISECONDS
            )
        }

        try {
            if (item.parts.size == 1) {
                smsManager.sendTextMessage(item.number, null, item.parts[0], sentIntents[0], null)
            } else {
                smsManager.sendMultipartTextMessage(item.number, null, item.parts, sentIntents, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки СМС через модем")
            failActiveOutboundSms(item.cmdId, "Ошибка модема Server при отправке СМС")
        }
    }

    private fun failActiveOutboundSms(cmdId: String, reason: String) {
        val completion = synchronized(smsTrackerLock) {
            activeSmsTimeoutFuture?.cancel(false)
            activeSmsTimeoutFuture = null
            outboundSmsQueue.onActiveFailure(cmdId, reason)
        } ?: return
        handleSmsCompleted(completion)
    }

    private fun handleSmsCompleted(res: OutboundSmsQueue.CompletionResult) {
        sendJson(JSONObject().apply {
            put("t", "SMS_SENT")
            put("id", res.cmdId)
            put("ok", res.ok)
            put("msg", res.message)
        })
        if (res.hasMoreQueued && isServerRunning) {
            acquirePulseWakeLock(SMS_INTER_SEND_DELAY_MS + 3_000L)
            synchronized(smsTrackerLock) {
                nextSmsDispatchFuture?.cancel(false)
                nextSmsDispatchFuture = watchdogExecutor.schedule(
                    { startNextOutboundSmsAfterCooldown() },
                    SMS_INTER_SEND_DELAY_MS,
                    TimeUnit.MILLISECONDS
                )
            }
        }
    }

    private fun startNextOutboundSmsAfterCooldown() {
        val next = synchronized(smsTrackerLock) {
            nextSmsDispatchFuture = null
            outboundSmsQueue.onCooldownFinished()
        } ?: return
        if (isServerRunning && !syncExecutor.isShutdown) {
            syncExecutor.execute { dispatchOutboundSms(next) }
        }
    }

    private fun isTelephonyRinging(): Boolean {
        return try {
            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION")
                tm.callState == TelephonyManager.CALL_STATE_RINGING
            } else {
                isIncomingCallRinging
            }
        } catch (_: Exception) {
            isIncomingCallRinging
        }
    }

    private fun dropIncomingCall(): Boolean {
        return try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                @Suppress("DEPRECATION")
                telecomManager.endCall()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сбросе вызова")
            false
        }
    }

    private fun startStatusReporter(socket: BluetoothSocket, channel: BtProtocol.SecureChannel) {
        thread(name = "BTStatusReporter") {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            while (isServerRunning && activeSocket === socket && socket.isConnected) {
                try {
                    val intervalMs = if (powerManager?.isInteractive != false) {
                        STATUS_POLL_ACTIVE_MS
                    } else {
                        STATUS_POLL_IDLE_MS
                    }
                    Thread.sleep(intervalMs)
                    if (isServerRunning && activeSocket === socket && socket.isConnected) {
                        sendStatusIfChanged(force = false, targetChannel = channel)
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun buildStatusJson(): JSONObject {
        val (batteryPct, isCharging) = getBatteryInfo()
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var operator = tm.networkOperatorName
        if (operator.isNullOrEmpty()) {
            operator = tm.simOperatorName
        }
        if (operator.isNullOrEmpty()) {
            operator = "Нет сети"
        }
        operator = operator.trim()

        val signalLevel = try {
            tm.signalStrength?.level ?: 0
        } catch (_: Exception) {
            0
        }

        val dataStatus = getMobileDataStatus(tm).trim()

        val ringing = isIncomingCallRinging && isTelephonyRinging()
        if (!ringing && isIncomingCallRinging) {
            isIncomingCallRinging = false
            lastIncomingNumber = ""
            lastIncomingName = ""
        }

        val currentHotspot = readHotspotState()
        if (currentHotspot == 0 || currentHotspot == 1) {
            synchronized(hotspotStateLock) {
                hotspotState = currentHotspot
            }
        }

        val currentMobileData = readMobileDataState()
        if (currentMobileData == 0 || currentMobileData == 1) {
            synchronized(mobileDataStateLock) {
                mobileDataState = currentMobileData
            }
        }

        return JSONObject().apply {
            put("t", "STATUS_UPDATE")
            put("battery", batteryPct)
            put("charging", isCharging)
            put("operator", operator)
            put("signal", signalLevel)
            put("data", dataStatus)
            put("callRinging", ringing)
            put("callerNumber", if (ringing) lastIncomingNumber.ifBlank { "Скрытый номер" } else "")
            put("callerName", if (ringing) lastIncomingName else "")
            put("hotspot", hotspotState)
            put("mobileData", mobileDataState)
        }
    }

    private fun sendStatusIfChanged(force: Boolean, targetChannel: BtProtocol.SecureChannel? = activeChannel) {
        val ch = targetChannel ?: return
        val statusObj = buildStatusJson()
        val statusString = statusObj.toString()
        val now = System.currentTimeMillis()

        val shouldSend: Boolean
        synchronized(statusSyncLock) {
            val heartbeatExpired = (now - lastStatusSentAtMs) >= 5 * 60_000L
            shouldSend = force || statusString != lastSentStatusJson || heartbeatExpired
            if (shouldSend) {
                lastSentStatusJson = statusString
                lastStatusSentAtMs = now
            }
        }

        if (shouldSend) {
            sendJson(statusObj, ch)
        }
    }

    private fun getBatteryInfo(): Pair<Int, Boolean> {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return Pair(pct, isCharging)
    }

    private fun getMobileNetworkTypeString(tm: TelephonyManager): String {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Моб. сеть"
        }
        return try {
            when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G/LTE"
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_UMTS -> "3G"
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_GPRS -> "2G/E"
                else -> "Моб. сеть"
            }
        } catch (_: Exception) {
            "Моб. сеть"
        }
    }

    private fun getMobileDataStatus(tm: TelephonyManager): String {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNet = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNet)

            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            when {
                isWifi -> "Wi-Fi"
                isCellular && hasInternet -> getMobileNetworkTypeString(tm)
                isCellular -> "Моб (нет инета)"
                hasInternet -> "В сети"
                else -> "Откл"
            }
        } catch (_: Exception) {
            "Неизвестно"
        }
    }

    private fun sendJson(json: JSONObject, targetChannel: BtProtocol.SecureChannel? = activeChannel) {
        sendRawPayload(json.toString(), targetChannel)
    }

    private fun sendRawPayload(payload: String, targetChannel: BtProtocol.SecureChannel? = activeChannel) {
        if (!isServerRunning || sendExecutor.isShutdown) return
        sendExecutor.execute {
            val ch = targetChannel ?: activeChannel ?: return@execute
            try {
                ch.sendText(payload)
            } catch (e: Exception) {
                Log.w(TAG, "Ошибка отправки кадра на Т1")
                if (ch === activeChannel) {
                    try { activeSocket?.close() } catch (_: Exception) {}
                }
            }
        }
    }

    private fun getContactName(phoneNumber: String?): String {
        if (phoneNumber.isNullOrBlank()) return ""
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ""
        }
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
            var contactName = ""
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val colIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (colIdx >= 0) {
                        contactName = cursor.getString(colIdx) ?: ""
                    }
                }
            }
            contactName
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка чтения имени контакта")
            ""
        }
    }

    private fun syncRecentSms(cmdId: String) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            sendAck(cmdId, "SYNC_5MIN", false, "На Server нет разрешения READ_SMS")
            return
        }

        val time5MinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L)
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val selection = "date >= ?"
        val selectionArgs = arrayOf(time5MinutesAgo.toString())
        val sortOrder = "_id ASC"

        var count = 0
        var hasMore = false
        try {
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val addrIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")

                while (cursor.moveToNext()) {
                    if (count >= MAX_SYNC_BATCH) {
                        hasMore = true
                        break
                    }
                    val dbId = if (idIdx >= 0) cursor.getLong(idIdx) else System.currentTimeMillis()
                    val senderNumber = (if (addrIdx >= 0) cursor.getString(addrIdx) else null) ?: "Неизвестный"
                    val messageBody = (if (bodyIdx >= 0) cursor.getString(bodyIdx) else null) ?: ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val name = getContactName(senderNumber)

                    val json = JSONObject().apply {
                        put("t", "SMS_RECV")
                        put("eventId", "sms_db_$dbId")
                        put("number", senderNumber)
                        put("name", name)
                        put("body", messageBody)
                        put("ts", date)
                        put("isSync", true)
                    }
                    sendJson(json)
                    count++
                }
            }
            sendSyncResult(cmdId, count, hasMore)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при синхронизации СМС")
            sendAck(cmdId, "SYNC_5MIN", false, "Ошибка чтения СМС на Server")
        }
    }

    private fun wasConfirmedLiveRecently(sender: String, body: String, dbDateMs: Long): Boolean {
        val matchingEventIds = mutableListOf<String>()
        synchronized(liveSmsLock) {
            for (rec in liveDeliveredSms) {
                if (rec.sender == sender && rec.body == body && kotlin.math.abs(rec.receivedAtMs - dbDateMs) < 90_000L) {
                    matchingEventIds.add(rec.eventId)
                }
            }
        }
        if (matchingEventIds.isEmpty()) return false
        synchronized(unackedQueueLock) {
            // Считаем доставленным только то живое SMS, для которого уже пришёл EVENT_ACK (не осталось в очереди unackedLiveSms)
            for (evId in matchingEventIds) {
                if (!unackedLiveSms.containsKey(evId)) {
                    return true
                }
            }
        }
        return false
    }

    private fun syncSmsSinceDisconnectInternal(
        fetchFromTime: Long,
        minSmsId: Long
    ): Pair<List<PendingServerEvent>, Boolean> {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            return Pair(emptyList(), false)
        }

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        val selection = "date >= ? AND _id > ?"
        val selectionArgs = arrayOf(fetchFromTime.toString(), minSmsId.toString())
        val sortOrder = "_id ASC"

        val events = mutableListOf<PendingServerEvent>()
        var hasMore = false
        try {
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex("_id")
                val addrIdx = cursor.getColumnIndex("address")
                val bodyIdx = cursor.getColumnIndex("body")
                val dateIdx = cursor.getColumnIndex("date")

                while (cursor.moveToNext()) {
                    val dbId = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val senderNumber = (if (addrIdx >= 0) cursor.getString(addrIdx) else null) ?: "Неизвестный"
                    val messageBody = (if (bodyIdx >= 0) cursor.getString(bodyIdx) else null) ?: ""
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()

                    if (date >= sessionConnectedAt - 5_000L && wasConfirmedLiveRecently(senderNumber, messageBody, date)) {
                        continue
                    }

                    if (events.size >= MAX_SYNC_BATCH) {
                        hasMore = true
                        break
                    }

                    val eventId = "sms_db_$dbId"
                    val name = getContactName(senderNumber)
                    val payload = JSONObject().apply {
                        put("t", "SMS_RECV")
                        put("eventId", eventId)
                        put("number", senderNumber)
                        put("name", name)
                        put("body", messageBody)
                        put("ts", date)
                        put("isSync", true)
                    }.toString()

                    events.add(PendingServerEvent(eventId, "SMS_SYNC", dbId, date, payload))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации СМС с момента обрыва")
        }
        return Pair(events, hasMore)
    }

    private fun syncMissedCallsSinceDisconnect(
        fetchFromTime: Long,
        minCallId: Long
    ): Pair<List<PendingServerEvent>, Boolean> {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return Pair(emptyList(), false)
        }

        val uri = CallLog.Calls.CONTENT_URI
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE
        )
        val selection = "${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls._ID} > ?"
        val selectionArgs = arrayOf(
            CallLog.Calls.MISSED_TYPE.toString(),
            fetchFromTime.toString(),
            minCallId.toString()
        )
        val sortOrder = "${CallLog.Calls._ID} ASC"

        val events = mutableListOf<PendingServerEvent>()
        var hasMore = false
        try {
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
                val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)

                while (cursor.moveToNext()) {
                    if (events.size >= MAX_SYNC_BATCH) {
                        hasMore = true
                        break
                    }
                    val dbId = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val rawNumber = if (numIdx >= 0) cursor.getString(numIdx) else null
                    val number = rawNumber?.takeIf { it.isNotBlank() } ?: "Скрытый номер"
                    var name = (if (nameIdx >= 0) cursor.getString(nameIdx) else null) ?: ""
                    if (name.isEmpty() && number != "Скрытый номер") {
                        name = getContactName(number)
                    }
                    val date = if (dateIdx >= 0) cursor.getLong(dateIdx) else System.currentTimeMillis()
                    val eventId = "call_db_$dbId"

                    val payload = JSONObject().apply {
                        put("t", "MISSED_CALL")
                        put("eventId", eventId)
                        put("number", number)
                        put("name", name)
                        put("ts", date)
                    }.toString()

                    events.add(PendingServerEvent(eventId, "CALL_SYNC", dbId, date, payload))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка синхронизации пропущенных звонков")
        }
        return Pair(events, hasMore)
    }

    private fun syncAllSinceDisconnect(cmdId: String) {
        val windowEndMs = System.currentTimeMillis()
        val fetchFromTime: Long
        val minSmsId: Long
        val minCallId: Long

        synchronized(syncAckLock) {
            fetchFromTime = if (lastDisconnectTime > 0L) lastDisconnectTime else (windowEndMs - 30 * 60 * 1000L)
            minSmsId = lastSyncedSmsId
            minCallId = lastSyncedCallId
            pendingDisconnectCollecting = true
            pendingDisconnectEventIds.clear()
            pendingDisconnectTargetTime = windowEndMs
            pendingDisconnectHadMore = false
        }

        val (smsEvents, smsHasMore) = syncSmsSinceDisconnectInternal(fetchFromTime, minSmsId)
        val (callEvents, callHasMore) = syncMissedCallsSinceDisconnect(fetchFromTime, minCallId)
        val allEvents = smsEvents + callEvents
        val hasMore = smsHasMore || callHasMore

        synchronized(syncAckLock) {
            for (ev in allEvents) {
                pendingEventAcks[ev.eventId] = ev
                pendingDisconnectEventIds.add(ev.eventId)
            }
            pendingDisconnectHadMore = hasMore
            pendingDisconnectCollecting = false

            if (allEvents.isEmpty() && !hasMore) {
                lastDisconnectTime = windowEndMs
                if (this::prefs.isInitialized) {
                    prefs.edit().putLong(KEY_LAST_DISCONNECT_TIME, windowEndMs).apply()
                }
                pendingDisconnectTargetTime = 0L
            }
        }

        for (ev in allEvents) {
            sendRawPayload(ev.payloadJson)
        }
        sendSyncResult(cmdId, allEvents.size, hasMore)
    }

    private fun sendSyncResult(cmdId: String, count: Int, hasMore: Boolean) {
        val result = if (count > 0) {
            JSONObject().apply {
                put("t", "SYNC_DONE")
                put("id", cmdId)
                put("count", count)
                put("hasMore", hasMore)
            }
        } else {
            JSONObject().apply {
                put("t", "SYNC_EMPTY")
                put("id", cmdId)
            }
        }
        sendJson(result)
    }

    private data class ExportContact(
        val name: String,
        val phone: String,
        val canonical: String,
        val inVisibleGroup: Boolean,
        val isPrimaryAccount: Boolean,
        val lastUpdatedMs: Long
    )

    private fun cleanExportPhone(raw: String): String {
        val sb = StringBuilder(raw.length)
        var prevSpace = false
        for (ch in raw) {
            if (Character.getType(ch) == Character.FORMAT.toInt()) continue
            when {
                ch == '\u00A0' || ch.isWhitespace() -> {
                    if (sb.isNotEmpty() && !prevSpace) {
                        sb.append(' ')
                        prevSpace = true
                    }
                }
                ch == '–' || ch == '—' -> {
                    sb.append('-')
                    prevSpace = false
                }
                else -> {
                    sb.append(ch)
                    prevSpace = false
                }
            }
        }
        return sb.toString().trim().take(40)
    }

    private fun canonicalContactPhone(raw: String): String {
        val sb = StringBuilder(16)
        for (ch in raw) {
            if (Character.getType(ch) == Character.FORMAT.toInt() || ch.isWhitespace()) continue
            if (sb.isEmpty() && ch == '+') {
                sb.append('+')
            } else if (ch in '0'..'9') {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun exportDigitsOnly(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            if (ch in '0'..'9') sb.append(ch)
        }
        return sb.toString()
    }

    private fun isPrimaryExportAccount(accountType: String?): Boolean {
        if (accountType.isNullOrBlank()) return true
        val low = accountType.lowercase(Locale.ROOT)
        return listOf(
            "whatsapp", "telegram", "viber", "signal", "skype",
            "imo", "botim", "bip", "wechat", "line", "snapchat"
        ).none { low.contains(it) }
    }

    private fun hasRealExportName(c: ExportContact): Boolean {
        return c.name.isNotBlank() &&
            c.name != c.phone &&
            canonicalContactPhone(c.name) != c.canonical
    }

    private fun isBetterExportContact(candidate: ExportContact, existing: ExportContact): Boolean {
        val candNamed = hasRealExportName(candidate)
        val existNamed = hasRealExportName(existing)
        if (candNamed != existNamed) return candNamed
        if (candidate.inVisibleGroup != existing.inVisibleGroup) return candidate.inVisibleGroup
        if (candidate.isPrimaryAccount != existing.isPrimaryAccount) return candidate.isPrimaryAccount
        if (candidate.lastUpdatedMs != existing.lastUpdatedMs) return candidate.lastUpdatedMs > existing.lastUpdatedMs
        val candPlus = candidate.canonical.startsWith("+")
        val existPlus = existing.canonical.startsWith("+")
        if (candPlus != existPlus) return candPlus
        val candFormatted = candidate.phone.any { it == ' ' || it == '-' || it == '(' || it == ')' }
        val currFormatted = existing.phone.any { it == ' ' || it == '-' || it == '(' || it == ')' }
        return candFormatted && !currFormatted
    }

    private fun mergeExportWinnerPlus(winner: ExportContact, loser: ExportContact): ExportContact {
        if (!winner.canonical.startsWith("+") &&
            loser.canonical.startsWith("+") &&
            exportDigitsOnly(winner.canonical) == exportDigitsOnly(loser.canonical)
        ) {
            return winner.copy(phone = loser.phone, canonical = loser.canonical)
        }
        return winner
    }

    private fun deduplicateExportContacts(raw: List<ExportContact>): List<ExportContact> {
        val bestByExactKey = LinkedHashMap<String, ExportContact>(raw.size)
        for (item in raw) {
            val digits = exportDigitsOnly(item.canonical)
            if (digits.length !in 3..15) continue
            val exactKey = if (digits.length >= 10) digits else item.canonical
            val existing = bestByExactKey[exactKey]
            bestByExactKey[exactKey] = if (existing == null) {
                item
            } else if (isBetterExportContact(item, existing)) {
                mergeExportWinnerPlus(item, existing)
            } else {
                mergeExportWinnerPlus(existing, item)
            }
        }

        val pass1 = bestByExactKey.values.toMutableList()
        if (pass1.size <= 1) return pass1

        val intlByLast10 = HashMap<String, MutableList<Int>>(pass1.size)
        for (i in pass1.indices) {
            val c = pass1[i]
            val digits = exportDigitsOnly(c.canonical)
            if (c.canonical.startsWith("+") && digits.length >= 11) {
                intlByLast10.getOrPut(digits.takeLast(10)) { ArrayList(2) }.add(i)
            }
        }

        val removed = BooleanArray(pass1.size)
        for (i in pass1.indices) {
            if (removed[i]) continue
            val local = pass1[i]
            if (local.canonical.startsWith("+")) continue
            val digits = exportDigitsOnly(local.canonical)
            val isLocal = digits.length == 10 || (digits.length == 11 && (digits.startsWith("0") || digits.startsWith("8")))
            if (!isLocal) continue

            val last10 = digits.takeLast(10)
            val intlMatches = intlByLast10[last10] ?: continue
            if (intlMatches.size != 1) continue

            val intlIdx = intlMatches[0]
            if (removed[intlIdx]) continue
            val intl = pass1[intlIdx]
            pass1[intlIdx] = if (isBetterExportContact(local, intl)) local else intl
            removed[i] = true
        }

        val out = ArrayList<ExportContact>(minOf(pass1.size, MAX_EXPORT_CONTACTS))
        for (i in pass1.indices) {
            if (!removed[i]) {
                out.add(pass1[i])
                if (out.size >= MAX_EXPORT_CONTACTS) break
            }
        }
        return out
    }

    private fun queryContactsForExport(): List<ExportContact> {
        val rawList = ArrayList<ExportContact>(256)
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val fullProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.IN_VISIBLE_GROUP,
            ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP,
            ContactsContract.RawContacts.ACCOUNT_TYPE
        )
        val basicProjection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val cursor = try {
            contentResolver.query(uri, fullProjection, null, null, null)
        } catch (_: IllegalArgumentException) {
            contentResolver.query(uri, basicProjection, null, null, null)
        }

        cursor?.use { c ->
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val visIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IN_VISIBLE_GROUP)
            val updIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_LAST_UPDATED_TIMESTAMP)
            val accIdx = c.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)

            while (c.moveToNext()) {
                val rawNumber = (if (numIdx >= 0) c.getString(numIdx) else null) ?: continue
                val cleanPhone = cleanExportPhone(rawNumber)
                val canonical = canonicalContactPhone(cleanPhone)
                val digitsCount = exportDigitsOnly(canonical).length
                if (digitsCount !in 3..15) continue

                val rawName = (if (nameIdx >= 0) c.getString(nameIdx) else null) ?: ""
                val cleanName = rawName.trim().take(120)
                val inVisible = if (visIdx >= 0) c.getInt(visIdx) != 0 else true
                val updatedAt = if (updIdx >= 0) c.getLong(updIdx) else 0L
                val accountType = if (accIdx >= 0) c.getString(accIdx) else null

                rawList.add(
                    ExportContact(
                        name = cleanName.ifEmpty { cleanPhone },
                        phone = cleanPhone.ifEmpty { canonical },
                        canonical = canonical,
                        inVisibleGroup = inVisible,
                        isPrimaryAccount = isPrimaryExportAccount(accountType),
                        lastUpdatedMs = updatedAt
                    )
                )
            }
        }
        return deduplicateExportContacts(rawList)
    }

    private fun sendContactsList(cmdId: String) {
        val targetChannel = activeChannel ?: return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            sendJson(JSONObject().apply {
                put("t", "CONTACTS_END")
                put("id", cmdId)
                put("ok", false)
                put("msg", "На Server нет разрешения READ_CONTACTS")
                put("total", 0)
            }, targetChannel)
            return
        }

        try {
            val collected = queryContactsForExport()

            var chunkIndex = 0
            var itemsArray = JSONArray()
            var approxBytes = 64
            var totalSent = 0

            for (contact in collected) {
                if (!isServerRunning || activeChannel !== targetChannel) return
                val itemObj = JSONObject().apply {
                    put("n", contact.name)
                    put("p", contact.phone)
                }
                val itemBytes = itemObj.toString().toByteArray(Charsets.UTF_8).size + 2
                if (itemsArray.length() >= CONTACTS_CHUNK_MAX_ITEMS ||
                    (itemsArray.length() > 0 && approxBytes + itemBytes > CONTACTS_CHUNK_MAX_BYTES)
                ) {
                    sendJson(JSONObject().apply {
                        put("t", "CONTACTS_CHUNK")
                        put("id", cmdId)
                        put("chunkIndex", chunkIndex++)
                        put("items", itemsArray)
                    }, targetChannel)
                    itemsArray = JSONArray()
                    approxBytes = 64
                }
                itemsArray.put(itemObj)
                approxBytes += itemBytes
                totalSent++
            }

            if (itemsArray.length() > 0) {
                if (!isServerRunning || activeChannel !== targetChannel) return
                sendJson(JSONObject().apply {
                    put("t", "CONTACTS_CHUNK")
                    put("id", cmdId)
                    put("chunkIndex", chunkIndex)
                    put("items", itemsArray)
                }, targetChannel)
            }

            if (!isServerRunning || activeChannel !== targetChannel) return
            sendJson(JSONObject().apply {
                put("t", "CONTACTS_END")
                put("id", cmdId)
                put("ok", true)
                put("total", totalSent)
            }, targetChannel)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения контактов")
            if (isServerRunning && activeChannel === targetChannel) {
                sendJson(JSONObject().apply {
                    put("t", "CONTACTS_END")
                    put("id", cmdId)
                    put("ok", false)
                    put("msg", "Ошибка чтения контактов на Server")
                    put("total", 0)
                }, targetChannel)
            }
        }
    }

    private fun updateState(stateText: String, connected: Boolean, clientName: String) {
        isConnected = connected
        currentStateText = stateText
        connectedClientLabel = clientName
        updateNotification(stateText)
        if (callback != null) {
            mainHandler.post {
                callback?.onServiceStateChanged(stateText, connected, clientName)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                STATUS_CHANNEL_ID,
                "Работа BTRouter Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Постоянное уведомление работы шлюза BTRouterServer"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)

            val hotspotTriggerChannel = NotificationChannel(
                HOTSPOT_TRIGGER_CHANNEL_ID,
                "Управление точкой доступа",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Служебные уведомления-триггеры для сценариев Samsung (BTR_HOTSPOT_ON / BTR_HOTSPOT_OFF)"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(hotspotTriggerChannel)

            val mobileDataTriggerChannel = NotificationChannel(
                MOBILE_DATA_TRIGGER_CHANNEL_ID,
                "Управление мобильным интернетом",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Служебные уведомления-триггеры для сценариев Samsung (BTR_MOBILE_DATA_ON / BTR_MOBILE_DATA_OFF)"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(mobileDataTriggerChannel)
        }
    }

    private fun applyHotspotState(normalized: Int, pushStatus: Boolean) {
        if (normalized != 0 && normalized != 1) return
        val changed = synchronized(hotspotStateLock) {
            val prev = hotspotState
            hotspotState = normalized
            prev != normalized
        }
        if (changed && pushStatus && isConnected) {
            sendStatusIfChanged(force = false)
        }
    }

    private fun updateHotspotStateFromApInt(rawState: Int, pushStatus: Boolean) {
        val normalized = when (rawState) {
            WIFI_AP_STATE_ENABLED -> 1
            WIFI_AP_STATE_DISABLED -> 0
            else -> return
        }
        applyHotspotState(normalized, pushStatus)
    }

    @Suppress("DEPRECATION")
    private fun collectUpstreamWifiInterfaces(): Set<String> {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return emptySet()
        val result = HashSet<String>()
        try {
            for (network in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(network) ?: continue
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
                val lp = cm.getLinkProperties(network) ?: continue
                lp.interfaceName?.takeIf { it.isNotBlank() }?.let { result.add(it) }
            }
        } catch (_: Throwable) {
        }
        return result
    }

    private fun readHotspotStateFromWifiManager(): Int {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return -1
            val getStateMethod = try {
                wifiManager.javaClass.getDeclaredMethod("getWifiApState").apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                wifiManager.javaClass.getMethod("getWifiApState")
            }
            when (getStateMethod.invoke(wifiManager) as? Int) {
                WIFI_AP_STATE_ENABLED -> return 1
                WIFI_AP_STATE_DISABLED -> return 0
            }
            val isEnabledMethod = try {
                wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled").apply { isAccessible = true }
            } catch (_: NoSuchMethodException) {
                wifiManager.javaClass.getMethod("isWifiApEnabled")
            }
            val enabled = isEnabledMethod.invoke(wifiManager) as? Boolean ?: return -1
            if (enabled) 1 else 0
        } catch (_: Throwable) {
            -1
        }
    }

    private fun readHotspotStateFromNetworkInterfaces(): Int {
        return try {
            val enumIfaces = NetworkInterface.getNetworkInterfaces() ?: return -1
            val snapshots = ArrayList<HotspotDetector.InterfaceSnapshot>()
            while (enumIfaces.hasMoreElements()) {
                val nif = enumIfaces.nextElement() ?: continue
                val name = nif.name ?: continue
                val isUp = try { nif.isUp } catch (_: Throwable) { false }
                val isLoopback = try { nif.isLoopback } catch (_: Throwable) { false }
                val isP2p = try { nif.isPointToPoint } catch (_: Throwable) { false }
                var hasSiteLocalIpv4 = false
                if (isUp && !isLoopback && !isP2p) {
                    val addrs = try { nif.inetAddresses } catch (_: Throwable) { null }
                    while (addrs != null && addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                            hasSiteLocalIpv4 = true
                            break
                        }
                    }
                }
                snapshots.add(
                    HotspotDetector.InterfaceSnapshot(
                        name = name,
                        isUp = isUp,
                        isLoopback = isLoopback,
                        isPointToPoint = isP2p,
                        hasSiteLocalIpv4 = hasSiteLocalIpv4
                    )
                )
            }
            val upstreamWifi = collectUpstreamWifiInterfaces()
            HotspotDetector.detectFromInterfaces(snapshots, upstreamWifi)
        } catch (_: Throwable) {
            -1
        }
    }

    private fun readHotspotState(tetherHint: Int = -1): Int {
        val wmState = readHotspotStateFromWifiManager()
        if (wmState == 0 || wmState == 1) return wmState

        if (tetherHint == 0 || tetherHint == 1) return tetherHint

        return readHotspotStateFromNetworkInterfaces()
    }

    private fun refreshHotspotState(pushStatus: Boolean, tetherHint: Int = -1) {
        val normalized = readHotspotState(tetherHint)
        if (normalized == 0 || normalized == 1) {
            applyHotspotState(normalized, pushStatus)
        }
    }

    private fun readMobileDataState(): Int {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                return if (tm.isDataEnabled) 1 else 0
            }
        } catch (_: Exception) {
        }
        return try {
            when (Settings.Global.getInt(contentResolver, "mobile_data", -1)) {
                1 -> 1
                0 -> 0
                else -> -1
            }
        } catch (_: Exception) {
            -1
        }
    }

    private fun refreshMobileDataState(pushStatus: Boolean) {
        val current = readMobileDataState()
        if (current != 0 && current != 1) return
        val changed = synchronized(mobileDataStateLock) {
            val prev = mobileDataState
            mobileDataState = current
            prev != current
        }
        if (changed && pushStatus && isConnected) {
            sendStatusIfChanged(force = false)
        }
    }

    private fun triggerHotspotRoutine(cmdId: String, enabled: Boolean) {
        triggerSamsungRoutine(
            cmdId = cmdId,
            cmdName = "SET_HOTSPOT",
            channelId = HOTSPOT_TRIGGER_CHANNEL_ID,
            keyword = if (enabled) HOTSPOT_TRIGGER_ON_KEYWORD else HOTSPOT_TRIGGER_OFF_KEYWORD,
            state = hotspotTriggerState,
            successMsg = if (enabled) {
                "Команда включения точки доступа отправлена на Server"
            } else {
                "Команда выключения точки доступа отправлена на Server"
            },
            errorMsg = "Ошибка отправки уведомления точки доступа на Server"
        )
        scheduleHotspotRechecks()
    }

    private fun scheduleHotspotRechecks() {
        val delays = longArrayOf(HOTSPOT_RECHECK_FIRST_MS, HOTSPOT_RECHECK_SECOND_MS)
        for (delayMs in delays) {
            mainHandler.postDelayed({
                if (isServerRunning && isConnected && !syncExecutor.isShutdown) {
                    syncExecutor.execute {
                        if (isServerRunning && isConnected) {
                            refreshHotspotState(pushStatus = true)
                        }
                    }
                }
            }, delayMs)
        }
    }

    private fun triggerMobileDataRoutine(cmdId: String, enabled: Boolean) {
        triggerSamsungRoutine(
            cmdId = cmdId,
            cmdName = "SET_MOBILE_DATA",
            channelId = MOBILE_DATA_TRIGGER_CHANNEL_ID,
            keyword = if (enabled) MOBILE_DATA_TRIGGER_ON_KEYWORD else MOBILE_DATA_TRIGGER_OFF_KEYWORD,
            state = mobileDataTriggerState,
            successMsg = if (enabled) {
                "Команда включения мобильного интернета отправлена на Server"
            } else {
                "Команда выключения мобильного интернета отправлена на Server"
            },
            errorMsg = "Ошибка отправки уведомления мобильного интернета на Server"
        )
    }

    private fun triggerSamsungRoutine(
        cmdId: String,
        cmdName: String,
        channelId: String,
        keyword: String,
        state: RoutineTriggerState,
        successMsg: String,
        errorMsg: String
    ) {
        if (!CompatUtils.hasPostNotificationsPermission(this)) {
            sendAck(cmdId, cmdName, false, "На Server нет разрешения POST_NOTIFICATIONS")
            return
        }

        acquirePulseWakeLock(4_500L)
        val notifId = state.notifIdGen.getAndIncrement()

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(keyword)
            .setContentText(keyword)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(false)
            .setAutoCancel(true)
            .build()

        try {
            synchronized(state.lock) {
                state.cancelRunnable?.let { mainHandler.removeCallbacks(it) }
                state.cancelRunnable = null
                if (state.lastNotifId >= 0) {
                    try {
                        NotificationManagerCompat.from(this).cancel(state.lastNotifId)
                    } catch (_: Exception) {
                    }
                    state.lastNotifId = -1
                }
            }

            NotificationManagerCompat.from(this).notify(notifId, notification)

            val cancelRunnable = Runnable {
                synchronized(state.lock) {
                    try {
                        NotificationManagerCompat.from(this).cancel(notifId)
                    } catch (_: Exception) {
                    }
                    if (state.lastNotifId == notifId) {
                        state.lastNotifId = -1
                        state.cancelRunnable = null
                    }
                }
            }

            synchronized(state.lock) {
                state.lastNotifId = notifId
                state.cancelRunnable = cancelRunnable
            }
            mainHandler.postDelayed(cancelRunnable, ROUTINE_TRIGGER_CLEANUP_MS)

            sendAck(cmdId, cmdName, true, successMsg)
        } catch (e: SecurityException) {
            Log.w(TAG, "Нет разрешения POST_NOTIFICATIONS для сценария $cmdName")
            sendAck(cmdId, cmdName, false, "На Server нет разрешения POST_NOTIFICATIONS")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка отправки уведомления сценария $cmdName")
            sendAck(cmdId, cmdName, false, errorMsg)
        }
    }

    private fun cancelPendingRoutineCleanup(state: RoutineTriggerState) {
        synchronized(state.lock) {
            state.cancelRunnable?.let { mainHandler.removeCallbacks(it) }
            state.cancelRunnable = null
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("BTRouter Server")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .build()
    }

    @Synchronized
    private fun updateNotification(text: String) {
        if (text == lastPostedNotifText) return
        if (!CompatUtils.hasPostNotificationsPermission(this)) return
        try {
            NotificationManagerCompat.from(this).notify(STATUS_NOTIFICATION_ID, buildNotification(text))
            lastPostedNotifText = text
        } catch (_: SecurityException) {
            Log.w(TAG, "Нет разрешения POST_NOTIFICATIONS для обновления статуса")
        }
    }

    private fun cancelHiddenCallPending() {
        hiddenCallRunnable?.let { mainHandler.removeCallbacks(it) }
        hiddenCallRunnable = null
    }

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
            acquirePulseWakeLock(4_000L)

            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            when (state) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    val wasAlreadyRinging = isIncomingCallRinging
                    isIncomingCallRinging = true

                    if (!number.isNullOrBlank()) {
                        cancelHiddenCallPending()
                        if (number != lastIncomingNumber) {
                            lastIncomingNumber = number
                            val name = getContactName(number)
                            lastIncomingName = name
                            sendJson(JSONObject().apply {
                                put("t", "CALL_RINGING")
                                put("number", number)
                                put("name", name)
                            })
                        }
                    } else if (!wasAlreadyRinging && !hiddenCallSentForSession) {
                        cancelHiddenCallPending()
                        val pendingHidden = Runnable {
                            if (isIncomingCallRinging && lastIncomingNumber.isEmpty() && !hiddenCallSentForSession) {
                                hiddenCallSentForSession = true
                                sendJson(JSONObject().apply {
                                    put("t", "CALL_RINGING")
                                    put("number", "Скрытый номер")
                                    put("name", "")
                                })
                            }
                        }
                        hiddenCallRunnable = pendingHidden
                        mainHandler.postDelayed(pendingHidden, 500L)
                    }
                }

                TelephonyManager.EXTRA_STATE_IDLE,
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    cancelHiddenCallPending()
                    val shouldNotifyEnded = isIncomingCallRinging
                    isIncomingCallRinging = false
                    lastIncomingNumber = ""
                    lastIncomingName = ""
                    hiddenCallSentForSession = false
                    if (shouldNotifyEnded) {
                        sendJson(JSONObject().put("t", "CALL_ENDED"))
                    }
                }
            }
        }
    }

    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != "android.provider.Telephony.SMS_RECEIVED") return
            acquirePulseWakeLock(4_000L)

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val receivedAtMs = System.currentTimeMillis()
            val grouped = LinkedHashMap<String, Pair<StringBuilder, Long>>()
            for (msg in messages) {
                if (msg == null) continue
                val sender = msg.displayOriginatingAddress?.takeIf { it.isNotBlank() }
                    ?: msg.originatingAddress?.takeIf { it.isNotBlank() }
                    ?: "Неизвестный"
                val partBody = msg.messageBody ?: ""
                val msgTs = if (msg.timestampMillis > 0L) msg.timestampMillis else receivedAtMs
                val existing = grouped[sender]
                if (existing == null) {
                    grouped[sender] = Pair(StringBuilder(partBody), msgTs)
                } else {
                    existing.first.append(partBody)
                }
            }

            for ((sender, pair) in grouped) {
                val fullBody = pair.first.toString()
                val ts = pair.second
                val name = getContactName(sender)
                val eventId = "sms_live_${receivedAtMs}_${sender.hashCode()}_${fullBody.hashCode()}"

                synchronized(liveSmsLock) {
                    if (liveDeliveredSms.size >= 50) {
                        liveDeliveredSms.removeFirst()
                    }
                    liveDeliveredSms.addLast(LiveSmsRecord(eventId, sender, fullBody, receivedAtMs))
                }

                val payload = JSONObject().apply {
                    put("t", "SMS_RECV")
                    put("eventId", eventId)
                    put("number", sender)
                    put("name", name)
                    put("body", fullBody)
                    put("ts", ts)
                    put("isSync", false)
                }.toString()

                val ev = PendingServerEvent(eventId, "LIVE_SMS", 0L, receivedAtMs, payload)
                enqueueUnackedLiveSms(ev)

                val ch = activeChannel
                if (isConnected && ch != null) {
                    sendRawPayload(payload, ch)
                }
            }
        }
    }

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    Log.d(TAG, "Bluetooth включён — перезапускаю сервер")
                    wakeServerLoop()
                }
                BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                    Log.d(TAG, "Bluetooth выключается — закрываю сокеты")
                    recordDisconnectTime()
                    closeActiveClientSession()
                    closeServerSocket()
                    updateState("Bluetooth на Server выключен", false, "")
                }
            }
        }
    }

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ACTION_SMS_SENT) return
            val cmdId = intent.getStringExtra(EXTRA_SMS_CMD_ID) ?: return
            if (cmdId.isEmpty()) return

            val code = resultCode
            val errDesc = when (code) {
                Activity.RESULT_OK -> null
                SmsManager.RESULT_ERROR_NO_SERVICE -> "Нет сотовой сети на Server"
                SmsManager.RESULT_ERROR_RADIO_OFF -> "Сотовый модуль Server выключен"
                SmsManager.RESULT_ERROR_NULL_PDU -> "Ошибка PDU модема"
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Сбой отправки оператором"
                else -> "Ошибка отправки (код $code)"
            }

            val completion = synchronized(smsTrackerLock) {
                val res = outboundSmsQueue.onPartResult(cmdId, errDesc)
                if (res != null) {
                    activeSmsTimeoutFuture?.cancel(false)
                    activeSmsTimeoutFuture = null
                }
                res
            } ?: return

            handleSmsCompleted(completion)
        }
    }

    private val wifiApStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isServerRunning || intent?.action != ACTION_WIFI_AP_STATE_CHANGED) return
            val state = intent.getIntExtra(EXTRA_WIFI_AP_STATE, -1)
            updateHotspotStateFromApInt(state, pushStatus = true)
        }
    }

    private val tetherStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isServerRunning || intent?.action != ACTION_TETHER_STATE_CHANGED) return
            val tetherList = try {
                intent.getStringArrayListExtra(EXTRA_ACTIVE_TETHER)
            } catch (_: Throwable) {
                null
            }
            val hint = HotspotDetector.detectFromTetherArray(tetherList)
            if (!syncExecutor.isShutdown) {
                syncExecutor.execute {
                    if (isServerRunning) {
                        refreshHotspotState(pushStatus = true, tetherHint = hint)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServerRunning = false
        cancelHiddenCallPending()
        cancelPendingRoutineCleanup(hotspotTriggerState)
        cancelPendingRoutineCleanup(mobileDataTriggerState)
        synchronized(smsTrackerLock) {
            activeSmsTimeoutFuture?.cancel(false)
            activeSmsTimeoutFuture = null
            nextSmsDispatchFuture?.cancel(false)
            nextSmsDispatchFuture = null
            outboundSmsQueue.clear()
        }

        closeActiveClientSession()
        closeServerSocket()
        wakeServerLoop()

        sendExecutor.shutdownNow()
        syncExecutor.shutdownNow()
        watchdogExecutor.shutdownNow()

        if (isInitialized) {
            try { unregisterReceiver(callReceiver) } catch (_: Exception) {}
            try { unregisterReceiver(smsReceiver) } catch (_: Exception) {}
            try { unregisterReceiver(btStateReceiver) } catch (_: Exception) {}
            try { unregisterReceiver(smsSentReceiver) } catch (_: Exception) {}
            try { unregisterReceiver(wifiApStateReceiver) } catch (_: Exception) {}
            try { unregisterReceiver(tetherStateReceiver) } catch (_: Exception) {}
        }
        mobileDataObserver?.let { observer ->
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
            }
            mobileDataObserver = null
        }

        releasePulseWakeLock()
    }
}
