package com.exantt.btrouterclient

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class BTRouterClientService : Service() {

    companion object {
        private const val TAG = "BT_CLIENT_SVC"

        const val ACTION_REFRESH_STATUS = "com.exantt.btrouterclient.ACTION_REFRESH_STATUS"
        const val ACTION_RESTORE_STATUS_NOTIFICATION = "com.exantt.btrouterclient.ACTION_RESTORE_STATUS_NOTIFICATION"
        const val ACTION_DECLINE_CALL = "com.exantt.btrouterclient.ACTION_DECLINE_CALL"
        const val ACTION_COPY_OTP = "com.exantt.btrouterclient.ACTION_COPY_OTP"
        const val EXTRA_OTP_CODE = "extra_otp_code"
        const val EXTRA_OPEN_CHAT_KEY = "extra_open_chat_key"
        const val EXTRA_OPEN_MISSED_CALLS = "extra_open_missed_calls"

        private const val STATUS_NOTIF_RESTORE_DELAY_MS = 400L

        const val PREFS_NAME = "bt_router_client_prefs"
        const val KEY_TARGET_MAC = "target_mac"
        const val KEY_TARGET_NAME = "target_name"
        const val KEY_SHARED_SECRET = "shared_secret"
        const val KEY_AUTO_COPY_OTP = "auto_copy_otp"
        const val KEY_SHOW_HOTSPOT_CONTROL = "show_hotspot_control"
        const val KEY_SHOW_MOBILE_DATA_CONTROL = "show_mobile_data_control"
        const val KEY_RECENT_SMS_CONTACTS = "recent_sms_contacts"
        const val KEY_LAST_CONTACTS_SOURCE = "last_contacts_source"
        private const val KEY_MISSED_CALLS_LAST_VIEWED_MS = "missed_calls_last_viewed_ms"
        const val SOURCE_CLIENT = "client"
        const val SOURCE_SERVER = "server"
        private const val KEY_PENDING_SMS_QUEUE = "pending_sms_queue"
        private const val AUTO_COPY_MAX_AGE_MS = 10 * 60_000L
        private const val MAX_RECENT_SMS_CONTACTS = 5
        private const val MAX_SERVER_CONTACTS = 5000

        const val STATUS_CHANNEL_ID = "T2StatusChannel"
        const val CALL_CHANNEL_ID = "CallChannel"
        const val SMS_CHANNEL_ID = "SmsChannel"
        const val MISSED_CHANNEL_ID = "MissedCallChannel"

        const val STATUS_NOTIFICATION_ID = 1001
        const val CALL_NOTIFICATION_ID = 1

        private const val HANDSHAKE_TIMEOUT_SEC = 15L
        private const val KEEPALIVE_INTERVAL_SEC = 60L
        private const val PENDING_SMS_TTL_MS = 30 * 60_000L
        private const val MAX_QUEUED_SMS = 20
        private const val MAX_CLIENT_SMS_CHARS = 1500
        private const val MAX_INCOMING_SMS_QUEUE = 50
        private const val MAX_SEEN_EVENTS = 500
        private const val EVENT_DEDUP_TTL_MS = 24 * 60 * 60_000L
        private const val HOTSPOT_PENDING_WINDOW_MS = 5_000L
        private const val MOBILE_DATA_PENDING_WINDOW_MS = 5_000L

        fun getTargetMac(context: Context): String? =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TARGET_MAC, null)

        fun getTargetName(context: Context): String? =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TARGET_NAME, null)

        fun getSavedSecret(context: Context): String? {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return SecretStorage.loadSecret(sp, KEY_SHARED_SECRET)
        }

        fun saveServerConfig(context: Context, mac: String, name: String, secret: String) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit()
                .putString(KEY_TARGET_MAC, mac.trim())
                .putString(KEY_TARGET_NAME, name.trim())
                .apply()
            SecretStorage.saveSecret(sp, KEY_SHARED_SECRET, secret.trim())
        }

        fun isAutoCopyOtpEnabled(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_COPY_OTP, true)

        fun setAutoCopyOtpEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_AUTO_COPY_OTP, enabled)
                .apply()
        }

        fun isHotspotControlVisible(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_HOTSPOT_CONTROL, false)

        fun setHotspotControlVisible(context: Context, visible: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_HOTSPOT_CONTROL, visible)
                .apply()
        }

        fun isMobileDataControlVisible(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SHOW_MOBILE_DATA_CONTROL, false)

        fun setMobileDataControlVisible(context: Context, visible: Boolean) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOW_MOBILE_DATA_CONTROL, visible)
                .apply()
        }

        fun getLastContactsSource(context: Context): String {
            val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_CONTACTS_SOURCE, SOURCE_CLIENT)
            return if (saved == SOURCE_SERVER) SOURCE_SERVER else SOURCE_CLIENT
        }

        fun setLastContactsSource(context: Context, source: String) {
            val resolved = if (source == SOURCE_SERVER) SOURCE_SERVER else SOURCE_CLIENT
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_CONTACTS_SOURCE, resolved)
                .apply()
        }

        fun getMissedCallsLastViewedMs(context: Context): Long =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_MISSED_CALLS_LAST_VIEWED_MS, 0L)

        fun markMissedCallsViewed(context: Context, viewedAtMs: Long = System.currentTimeMillis()) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_MISSED_CALLS_LAST_VIEWED_MS, viewedAtMs)
                .apply()
        }

        fun getRecentSmsContacts(context: Context): List<ContactsHelper.ContactEntry> {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_RECENT_SMS_CONTACTS, null) ?: return emptyList()
            val list = ArrayList<ContactsHelper.ContactEntry>(MAX_RECENT_SMS_CONTACTS)
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    if (list.size >= MAX_RECENT_SMS_CONTACTS) break
                    val obj = arr.optJSONObject(i) ?: continue
                    val rawPhone = obj.optString("p", "")
                    val displayPhone = ContactsHelper.cleanDisplayPhone(rawPhone)
                    val canonical = ContactsHelper.canonicalPhoneKey(obj.optString("np", displayPhone))
                    if (ContactsHelper.digitsOnly(canonical).length !in 3..15) continue
                    val name = obj.optString("n", "").trim().take(120)
                    list.add(
                        ContactsHelper.ContactEntry(
                            name = name,
                            phone = displayPhone.ifEmpty { canonical },
                            normalizedPhone = canonical
                        )
                    )
                }
            } catch (_: Exception) {
                return emptyList()
            }
            return list
        }

        fun recordRecentSmsContact(context: Context, rawNumber: String, displayName: String? = null) {
            val displayPhone = ContactsHelper.cleanDisplayPhone(rawNumber)
            val canonical = ContactsHelper.canonicalPhoneKey(rawNumber)
            if (ContactsHelper.digitsOnly(canonical).length !in 3..15) return

            val existing = getRecentSmsContacts(context)
            var preservedName = displayName?.trim()?.take(120) ?: ""
            if (preservedName.isEmpty()) {
                val old = existing.firstOrNull { it.normalizedPhone == canonical }
                if (old != null && old.name.isNotBlank()) {
                    preservedName = old.name
                }
            }

            val updated = ArrayList<ContactsHelper.ContactEntry>(MAX_RECENT_SMS_CONTACTS)
            updated.add(
                ContactsHelper.ContactEntry(
                    name = preservedName,
                    phone = displayPhone.ifEmpty { canonical },
                    normalizedPhone = canonical
                )
            )
            for (item in existing) {
                if (updated.size >= MAX_RECENT_SMS_CONTACTS) break
                if (item.normalizedPhone != canonical) {
                    updated.add(item)
                }
            }

            val arr = JSONArray()
            for (entry in updated) {
                arr.put(JSONObject().apply {
                    put("n", entry.name)
                    put("p", entry.phone)
                    put("np", entry.normalizedPhone)
                })
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_RECENT_SMS_CONTACTS, arr.toString())
                .apply()
        }
    }

    data class ActiveSms(
        val eventId: String,
        val number: String,
        val senderDisplay: String,
        val body: String,
        val otpCode: String? = null,
        val otpAutoCopied: Boolean = false,
        val senderName: String = ""
    )

    data class SmsDispatchResult(
        val ok: Boolean,
        val message: String,
        val chatKey: String? = null,
        val cmdId: String? = null
    )

    interface ServiceCallback {
        fun onStatusUpdated(batteryPct: Int, isCharging: Boolean, operator: String, signalLevel: Int, dataStatus: String)
        fun onConnectionStateChanged(connected: Boolean, statusMessage: String)
        fun onIncomingCall(callerDisplay: String)
        fun onCallEnded()
        fun onSmsQueueChanged(activeSms: ActiveSms?, totalCount: Int) {}
        fun onSyncDone(count: Int, hasMore: Boolean)
        fun onSyncEmpty()
        fun onStatusMessage(message: String)
        fun onCommandResult(cmdId: String, cmd: String, ok: Boolean, message: String, restoredText: String? = null)
        fun onServerContactsLoaded(ok: Boolean, contacts: List<ContactsHelper.ContactEntry>, errorMessage: String?) {}
        fun onHotspotStateChanged(hotspotState: Int, pendingTarget: Int, requestedState: Int) {}
        fun onMobileDataStateChanged(mobileDataState: Int, pendingTarget: Int, requestedState: Int) {}
    }

    inner class LocalBinder : Binder() {
        fun getService(): BTRouterClientService = this@BTRouterClientService
    }

    private sealed class ConnectionStatus {
        object Preparing : ConnectionStatus()
        object NeedConfigShort : ConnectionStatus()
        object NeedConfigDetail : ConnectionStatus()
        object NoBtPermission : ConnectionStatus()
        object BtDisabled : ConnectionStatus()
        object ServerNotBonded : ConnectionStatus()
        data class Connecting(val targetLabel: String) : ConnectionStatus()
        object Connected : ConnectionStatus()
        object ConnectionLost : ConnectionStatus()
        object AuthFailed : ConnectionStatus()
        object SecurityError : ConnectionStatus()
        object Searching : ConnectionStatus()
        object Refreshing : ConnectionStatus()
        object SearchingAndConnecting : ConnectionStatus()
    }

    private data class QueuedSms(
        val cmdId: String,
        val number: String,
        val body: String,
        val createdAtMs: Long = System.currentTimeMillis()
    )

    private data class SeenEntry(
        val expiresAtMs: Long
    )

    private val binder = LocalBinder()
    @Volatile
    var callback: ServiceCallback? = null

    @Volatile
    private var localizedContext: Context? = null

    private lateinit var prefs: SharedPreferences
    private lateinit var chatRepo: ChatRepository
    private var bluetoothAdapter: BluetoothAdapter? = null
    @Volatile
    private var activeSocket: BluetoothSocket? = null
    @Volatile
    private var activeChannel: BtProtocol.SecureChannel? = null
    @Volatile
    private var isInitialized = false
    @Volatile
    private var isRunning = true

    private var wakeLock: PowerManager.WakeLock? = null
    @Volatile
    private var wakeLockExpiresAtMs = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val sendExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    private val reconnectLock = Object()
    @Volatile
    private var immediateReconnectRequested = false
    @Volatile
    private var failedConnectAttempts = 0
    @Volatile
    private var authFailedPause = false

    @Volatile
    private var isScreenOn = true
    @Volatile
    private var hasPendingNotificationUpdate = false
    @Volatile
    private var lastStatusReceivedTime = 0L
    @Volatile
    private var lastServerActivityMs = 0L
    @Volatile
    private var missedPongs = 0

    private var pendingNotifTitle = ""
    private var pendingNotifContent = "🔋 --%   📶 --   🌐 --"
    private var pendingNotifDetail = ""

    private var lastPostedNotifTitle = ""
    private var lastPostedNotifContent = ""
    private var lastPostedNotifDetail = ""

    @Volatile
    private var connectionStatus: ConnectionStatus = ConnectionStatus.Preparing
    @Volatile
    var isConnected: Boolean = false
        private set
    @Volatile
    var lastBatteryPct: Int = -1
        private set
    @Volatile
    var lastIsCharging: Boolean = false
        private set
    @Volatile
    var lastOperator: String = ""
        private set
    @Volatile
    var lastSignalLevel: Int = 0
        private set
    private inner class RemoteToggleTracker(
        private val commandName: String,
        private val pendingWindowMs: Long,
        private val noConnRes: Int,
        private val turningOnRes: Int,
        private val turningOffRes: Int,
        private val notifyChange: (ServiceCallback, Int, Int, Int) -> Unit
    ) {
        val lock = Any()
        @Volatile var actualState: Int = -1
        @Volatile var requestedState: Int = -1
        @Volatile var pendingTarget: Int = -1
        @Volatile var pendingUntilMs: Long = 0L
        var timeoutRunnable: Runnable? = null

        fun applyStatus(rawValue: Int): Triple<Int, Int, Int> {
            val normalized = if (rawValue == 0 || rawValue == 1) rawValue else -1
            synchronized(lock) {
                actualState = normalized
                if (normalized == 0 || normalized == 1) {
                    if (pendingTarget == normalized || System.currentTimeMillis() >= pendingUntilMs) {
                        pendingTarget = -1
                        pendingUntilMs = 0L
                        timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                        timeoutRunnable = null
                    }
                }
                return Triple(actualState, pendingTarget, requestedState)
            }
        }

        fun onAckFailed() {
            val stateSnap: Int
            synchronized(lock) {
                pendingTarget = -1
                pendingUntilMs = 0L
                requestedState = -1
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                timeoutRunnable = null
                stateSnap = actualState
            }
            notifyUi { notifyChange(it, stateSnap, -1, -1) }
        }

        fun reset() {
            synchronized(lock) {
                actualState = -1
                requestedState = -1
                pendingTarget = -1
                pendingUntilMs = 0L
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                timeoutRunnable = null
            }
            notifyUi { notifyChange(it, -1, -1, -1) }
        }

        fun cancelTimer() {
            synchronized(lock) {
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                timeoutRunnable = null
            }
        }

        fun dispatch(enabled: Boolean): Pair<Boolean, String> {
            if (!isConnected || activeChannel == null) {
                return Pair(false, str(noConnRes))
            }
            val cmdId = nextCmdId()
            val dispatched = sendJson(JSONObject().apply {
                put("t", commandName)
                put("id", cmdId)
                put("enabled", enabled)
            })
            if (!dispatched) {
                return Pair(false, str(R.string.error_no_active_channel))
            }
            val target = if (enabled) 1 else 0
            val runnable = object : Runnable {
                override fun run() {
                    val curState: Int
                    val curRequested: Int
                    var cleared = false
                    synchronized(lock) {
                        if (timeoutRunnable === this) {
                            pendingTarget = -1
                            pendingUntilMs = 0L
                            timeoutRunnable = null
                            cleared = true
                        }
                        curState = actualState
                        curRequested = requestedState
                    }
                    if (cleared) {
                        if (isConnected && activeChannel != null) {
                            sendJson(JSONObject().apply {
                                put("t", "GET_STATUS")
                                put("id", nextCmdId())
                            })
                        }
                        notifyUi { notifyChange(it, curState, -1, curRequested) }
                    }
                }
            }
            val curActual: Int
            synchronized(lock) {
                requestedState = target
                pendingTarget = target
                pendingUntilMs = System.currentTimeMillis() + pendingWindowMs
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                timeoutRunnable = runnable
                curActual = actualState
            }
            mainHandler.postDelayed(runnable, pendingWindowMs + 100L)
            notifyUi { notifyChange(it, curActual, target, target) }
            return Pair(true, str(if (enabled) turningOnRes else turningOffRes))
        }
    }

    private val hotspotTracker = RemoteToggleTracker(
        commandName = "SET_HOTSPOT",
        pendingWindowMs = HOTSPOT_PENDING_WINDOW_MS,
        noConnRes = R.string.hotspot_no_connection,
        turningOnRes = R.string.hotspot_turning_on_status,
        turningOffRes = R.string.hotspot_turning_off_status,
        notifyChange = { cb, state, pending, requested -> cb.onHotspotStateChanged(state, pending, requested) }
    )

    private val mobileDataTracker = RemoteToggleTracker(
        commandName = "SET_MOBILE_DATA",
        pendingWindowMs = MOBILE_DATA_PENDING_WINDOW_MS,
        noConnRes = R.string.mobile_data_no_connection,
        turningOnRes = R.string.mobile_data_turning_on_status,
        turningOffRes = R.string.mobile_data_turning_off_status,
        notifyChange = { cb, state, pending, requested -> cb.onMobileDataStateChanged(state, pending, requested) }
    )

    @Volatile
    var lastDataStatus: String = ""
        private set
    val lastHotspotState: Int
        get() = hotspotTracker.actualState
    val lastRequestedHotspotState: Int
        get() = hotspotTracker.requestedState
    val hotspotPendingTarget: Int
        get() = hotspotTracker.pendingTarget
    val lastMobileDataState: Int
        get() = mobileDataTracker.actualState
    val lastRequestedMobileDataState: Int
        get() = mobileDataTracker.requestedState
    val mobileDataPendingTarget: Int
        get() = mobileDataTracker.pendingTarget
    @Volatile
    var lastStatusText: String = ""
        private set

    @Volatile
    var isCallActive: Boolean = false
        private set
    @Volatile
    var currentCallerDisplay: String = ""
        private set
    @Volatile
    private var currentCallerRawNumber: String = ""
    @Volatile
    private var currentCallerName: String = ""

    private val sessionIdPrefix = java.util.UUID.randomUUID().toString().take(8)
    private val cmdIdGen = AtomicInteger(1)
    private val dynamicNotifIdGen = AtomicInteger(2000)
    private var restoreNotifRunnable: Runnable? = null

    private fun nextCmdId(): String = "c-$sessionIdPrefix-${cmdIdGen.getAndIncrement()}"

    private val incomingSmsLock = Any()
    private val incomingSmsQueue = ArrayDeque<ActiveSms>()

    private val queueLock = Any()
    private val pendingSmsQueue = ArrayDeque<QueuedSms>()
    private val inFlightSms = ConcurrentHashMap<String, QueuedSms>()

    private val seenEventsLock = Any()
    private val seenEvents = LinkedHashMap<String, SeenEntry>(64, 0.75f, false)

    private val serverContactsLock = Any()
    private var cachedServerContacts: List<ContactsHelper.ContactEntry> = emptyList()
    private var hasLoadedServerContacts: Boolean = false
    private var lastServerContactsLoadedAtMs: Long = 0L
    private var activeContactsReqId: String? = null
    private val incomingContactsBuffer = ArrayList<ContactsHelper.ContactEntry>()

    private fun locContext(): Context =
        localizedContext ?: AppLocale.wrap(this).also { localizedContext = it }

    private fun str(@StringRes id: Int): String = locContext().getString(id)

    private fun str(@StringRes id: Int, vararg args: Any): String = locContext().getString(id, *args)

    private fun formatConnectionStatus(state: ConnectionStatus): String {
        return when (state) {
            ConnectionStatus.Preparing -> str(R.string.status_preparing)
            ConnectionStatus.NeedConfigShort -> str(R.string.status_configure_server_gear)
            ConnectionStatus.NeedConfigDetail -> str(R.string.status_configure_server_detail)
            ConnectionStatus.NoBtPermission -> str(R.string.status_no_bt_permission)
            ConnectionStatus.BtDisabled -> str(R.string.status_bt_disabled)
            ConnectionStatus.ServerNotBonded -> str(R.string.status_server_not_bonded)
            is ConnectionStatus.Connecting -> str(R.string.status_connecting_to, state.targetLabel)
            ConnectionStatus.Connected -> str(R.string.status_connected_encrypted)
            ConnectionStatus.ConnectionLost -> str(R.string.status_connection_lost)
            ConnectionStatus.AuthFailed -> str(R.string.status_auth_failed)
            ConnectionStatus.SecurityError -> str(R.string.status_security_error)
            ConnectionStatus.Searching -> str(R.string.status_searching_server)
            ConnectionStatus.Refreshing -> str(R.string.status_refreshing_server)
            ConnectionStatus.SearchingAndConnecting -> str(R.string.status_searching_and_connecting)
        }
    }

    private fun buildCallerDisplay(rawNumber: String, name: String): String {
        val localizedNumber = ServerMessageLocalizer.localizeCallerNumber(
            rawNumber,
            AppLocale.isEnglish(locContext())
        )
        return if (name.isNotEmpty()) "$name\n$localizedNumber" else localizedNumber
    }

    private fun repo(): ChatRepository {
        if (!this::chatRepo.isInitialized) {
            chatRepo = ChatRepository.getInstance(this)
        }
        return chatRepo
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        chatRepo = ChatRepository.getInstance(this)
        localizedContext = AppLocale.wrap(this)
        pendingNotifTitle = str(R.string.notif_title_waiting)
        pendingNotifDetail = str(R.string.status_waiting_connection)
        lastStatusText = formatConnectionStatus(ConnectionStatus.Preparing)

        createNotificationChannels()

        // Безопасный выход без ForegroundServiceDidNotStartInTimeException / SecurityException
        if (!CompatUtils.hasBluetoothConnectPermission(this)) {
            Log.w(TAG, "Нет разрешения BLUETOOTH_CONNECT — остановка клиентской службы")
            try {
                val fallbackNotification = buildStatusNotification(
                    title = str(R.string.notif_title_permission_required),
                    contentText = "🔋 --%   📶 --   🌐 --",
                    detailText = str(R.string.notif_detail_grant_bt)
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(STATUS_NOTIFICATION_ID, fallbackNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
                } else {
                    startForeground(STATUS_NOTIFICATION_ID, fallbackNotification)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
            } catch (_: Exception) {
            }
            stopSelf()
            return
        }

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadPendingSmsQueue()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BTRouterClient:ServiceWakeLock").apply {
            setReferenceCounted(false)
        }
        isScreenOn = powerManager.isInteractive

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        ContextCompat.registerReceiver(
            this,
            btStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_EXPORTED
        )

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            screenFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        if (!hasServerConfig()) {
            pendingNotifTitle = str(R.string.notif_title_setup_required)
            pendingNotifDetail = str(R.string.notif_detail_setup_required)
            connectionStatus = ConnectionStatus.NeedConfigShort
            lastStatusText = formatConnectionStatus(connectionStatus)
        }

        val initialNotification = buildStatusNotification(
            title = pendingNotifTitle,
            contentText = pendingNotifContent,
            detailText = pendingNotifDetail
        )
        lastPostedNotifTitle = pendingNotifTitle
        lastPostedNotifContent = pendingNotifContent
        lastPostedNotifDetail = pendingNotifDetail

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(STATUS_NOTIFICATION_ID, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(STATUS_NOTIFICATION_ID, initialNotification)
        }

        isInitialized = true
        thread(name = "BTConnectionLoop") { runConnectionLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isInitialized) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_REFRESH_STATUS -> refreshStatus()
            ACTION_RESTORE_STATUS_NOTIFICATION -> scheduleStatusNotificationRestore()
            ACTION_DECLINE_CALL -> {
                acquirePulseWakeLock(2500L, lockFloor = false)
                declineCall()
            }
            null -> if (intent != null) ensureStatusNotificationVisible()
        }
        return START_STICKY
    }

    private fun scheduleStatusNotificationRestore() {
        if (!isRunning || !isInitialized || !CompatUtils.hasPostNotificationsPermission(this)) return
        acquirePulseWakeLock(1_500L, lockFloor = false)
        restoreNotifRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            restoreNotifRunnable = null
            if (!isRunning || !isInitialized) return@Runnable
            updateStatusNotification(
                title = pendingNotifTitle,
                contentText = pendingNotifContent,
                detailText = pendingNotifDetail,
                force = true
            )
        }
        restoreNotifRunnable = runnable
        mainHandler.postDelayed(runnable, STATUS_NOTIF_RESTORE_DELAY_MS)
    }

    fun getTargetMac(): String? = getTargetMac(this)

    fun getTargetName(): String? = getTargetName(this)

    fun getSavedSecret(): String? = getSavedSecret(this)

    fun hasServerConfig(): Boolean {
        val mac = getTargetMac()
        val secret = getSavedSecret()
        return !mac.isNullOrBlank() && !secret.isNullOrBlank() && BtProtocol.isValidSecret(secret)
    }

    fun onConfigUpdated() {
        authFailedPause = false
        try { activeSocket?.close() } catch (_: Exception) {}
        activeSocket = null
        activeChannel = null
        triggerImmediateReconnect()
    }

    fun saveServerConfig(mac: String, name: String, secret: String) {
        saveServerConfig(this, mac, name, secret)
        onConfigUpdated()
    }

    fun getCurrentActiveSms(): ActiveSms? {
        synchronized(incomingSmsLock) {
            return incomingSmsQueue.firstOrNull()
        }
    }

    fun getPendingIncomingSmsCount(): Int {
        synchronized(incomingSmsLock) {
            return incomingSmsQueue.size
        }
    }

    fun dismissCurrentSms(expectedEventId: String? = null): ActiveSms? {
        val next: ActiveSms?
        val count: Int
        synchronized(incomingSmsLock) {
            val head = incomingSmsQueue.firstOrNull()
            if (head != null && (expectedEventId == null || head.eventId == expectedEventId)) {
                incomingSmsQueue.removeFirst()
            }
            next = incomingSmsQueue.firstOrNull()
            count = incomingSmsQueue.size
        }
        notifyUi { it.onSmsQueueChanged(next, count) }
        return next
    }

    private fun loadPendingSmsQueue() {
        val raw = prefs.getString(KEY_PENDING_SMS_QUEUE, null) ?: return
        val now = System.currentTimeMillis()
        var dirty = false
        synchronized(queueLock) {
            pendingSmsQueue.clear()
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val cmdId = obj.optString("cmdId", "")
                    val number = obj.optString("number", "")
                    val body = obj.optString("body", "")
                    val createdAt = obj.optLong("createdAtMs", 0L)
                    if (cmdId.isEmpty() || number.isEmpty() || body.isEmpty() || (now - createdAt > PENDING_SMS_TTL_MS)) {
                        dirty = true
                        continue
                    }
                    pendingSmsQueue.addLast(QueuedSms(cmdId, number, body, createdAt))
                }
            } catch (_: Exception) {
                pendingSmsQueue.clear()
                dirty = true
            }
            if (dirty) {
                persistPendingSmsQueueLocked()
            }
        }
    }

    private fun persistPendingSmsQueueLocked() {
        if (!this::prefs.isInitialized) return
        val arr = JSONArray()
        for (sms in pendingSmsQueue) {
            arr.put(JSONObject().apply {
                put("cmdId", sms.cmdId)
                put("number", sms.number)
                put("body", sms.body)
                put("createdAtMs", sms.createdAtMs)
            })
        }
        prefs.edit().putString(KEY_PENDING_SMS_QUEUE, arr.toString()).apply()
    }

    @Synchronized
    private fun acquirePulseWakeLock(timeoutMs: Long, lockFloor: Boolean) {
        try {
            val now = System.currentTimeMillis()
            val desiredExpiry = now + timeoutMs
            if (desiredExpiry > wakeLockExpiresAtMs) {
                if (lockFloor) {
                    wakeLockExpiresAtMs = desiredExpiry
                }
                wakeLock?.acquire(timeoutMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка захвата импульсного WakeLock")
        }
    }

    @Synchronized
    private fun releasePulseWakeLock(force: Boolean) {
        try {
            if (!force && System.currentTimeMillis() < wakeLockExpiresAtMs) {
                return
            }
            wakeLockExpiresAtMs = 0L
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка освобождения WakeLock")
        }
    }

    private fun getReconnectDelayMs(attempts: Int): Long {
        if (authFailedPause) return 60_000L
        return when {
            attempts <= 3 -> 3_000L
            attempts <= 6 -> 15_000L
            attempts <= 10 -> 30_000L
            else -> 60_000L
        }
    }

    private fun waitBeforeReconnect(delayMs: Long) {
        synchronized(reconnectLock) {
            if (!isRunning) return
            if (immediateReconnectRequested) {
                immediateReconnectRequested = false
                return
            }
            try {
                if (delayMs <= 0L) {
                    reconnectLock.wait()
                } else {
                    reconnectLock.wait(delayMs)
                }
            } catch (_: InterruptedException) {
            }
            immediateReconnectRequested = false
        }
    }

    fun triggerImmediateReconnect() {
        synchronized(reconnectLock) {
            authFailedPause = false
            failedConnectAttempts = 0
            immediateReconnectRequested = true
            reconnectLock.notifyAll()
        }
    }

    private fun runConnectionLoop() {
        while (isRunning) {
            var currentSocket: BluetoothSocket? = null
            var skipFinallyWait = false
            var keepaliveFuture: ScheduledFuture<*>? = null
            try {
                if (!CompatUtils.hasBluetoothConnectPermission(this)) {
                    notifyDisconnected(ConnectionStatus.NoBtPermission)
                    waitBeforeReconnect(10_000L)
                    skipFinallyWait = true
                    continue
                }

                val adapter = bluetoothAdapter
                if (adapter == null || !adapter.isEnabled) {
                    notifyDisconnected(ConnectionStatus.BtDisabled)
                    waitBeforeReconnect(5 * 60_000L)
                    skipFinallyWait = true
                    continue
                }

                val targetMac = getTargetMac()
                val secret = getSavedSecret()
                if (targetMac.isNullOrBlank() || secret.isNullOrBlank() || !BtProtocol.isValidSecret(secret)) {
                    notifyDisconnected(ConnectionStatus.NeedConfigDetail)
                    waitBeforeReconnect(5 * 60_000L)
                    skipFinallyWait = true
                    continue
                }

                val serverDevice: BluetoothDevice? = try {
                    adapter.bondedDevices?.find { it.address.equals(targetMac, ignoreCase = true) }
                } catch (_: SecurityException) {
                    null
                }

                if (serverDevice == null) {
                    notifyDisconnected(ConnectionStatus.ServerNotBonded)
                    waitBeforeReconnect(30_000L)
                    skipFinallyWait = true
                    continue
                }

                val targetLabel = getTargetName()?.ifBlank { null } ?: "Server"
                notifyDisconnected(ConnectionStatus.Connecting(targetLabel))

                if (CompatUtils.hasBluetoothScanPermission(this)) {
                    try {
                        adapter.cancelDiscovery()
                    } catch (_: SecurityException) {
                    }
                }

                val channel: BtProtocol.SecureChannel
                acquirePulseWakeLock(15_000L, lockFloor = false)
                var handshakeWatchdog: ScheduledFuture<*>? = null
                try {
                    currentSocket = serverDevice.createRfcommSocketToServiceRecord(BtProtocol.APP_UUID)
                    val socketToGuard = currentSocket
                    handshakeWatchdog = scheduler.schedule({
                        Log.w(TAG, "Таймаут подключения/рукопожатия клиента — закрываю сокет")
                        try { socketToGuard.close() } catch (_: Exception) {}
                    }, HANDSHAKE_TIMEOUT_SEC, TimeUnit.SECONDS)

                    currentSocket.connect()

                    val input = DataInputStream(BufferedInputStream(currentSocket.inputStream))
                    val output = DataOutputStream(BufferedOutputStream(currentSocket.outputStream))
                    val masterKey = BtProtocol.deriveMasterKey(secret)
                    channel = BtProtocol.performClientHandshake(input, output, masterKey)
                } finally {
                    handshakeWatchdog?.cancel(false)
                    releasePulseWakeLock(force = false)
                }

                authFailedPause = false
                failedConnectAttempts = 0
                activeSocket = currentSocket
                activeChannel = channel
                lastServerActivityMs = System.currentTimeMillis()
                missedPongs = 0

                notifyConnected(ConnectionStatus.Connected)

                sendJson(JSONObject().apply {
                    put("t", "GET_STATUS")
                    put("id", nextCmdId())
                })
                flushPendingSmsQueue()

                val guardedSocket = currentSocket
                keepaliveFuture = scheduler.scheduleAtFixedRate({
                    if (!isRunning || activeSocket !== guardedSocket || !isConnected) return@scheduleAtFixedRate
                    val now = System.currentTimeMillis()
                    if (now - lastServerActivityMs < 55_000L) {
                        missedPongs = 0
                        return@scheduleAtFixedRate
                    }
                    if (missedPongs >= 2) {
                        Log.w(TAG, "Нет ответа на 2 PING подряд — закрываю мёртвый сокет")
                        try { guardedSocket.close() } catch (_: Exception) {}
                        return@scheduleAtFixedRate
                    }
                    missedPongs++
                    acquirePulseWakeLock(3_000L, lockFloor = false)
                    sendJson(JSONObject().apply {
                        put("t", "PING")
                        put("ts", now)
                    })
                }, KEEPALIVE_INTERVAL_SEC, KEEPALIVE_INTERVAL_SEC, TimeUnit.SECONDS)

                while (isRunning && isConnected) {
                    val frameText = channel.receiveText()
                    lastServerActivityMs = System.currentTimeMillis()
                    missedPongs = 0
                    handleIncomingFrameWithWakeLock(frameText)
                }

                if (isRunning) {
                    notifyDisconnected(ConnectionStatus.ConnectionLost)
                }
            } catch (authEx: BtProtocol.AuthException) {
                failedConnectAttempts++
                authFailedPause = true
                Log.w(TAG, "Ошибка ключа сопряжения при авторизации с Т2")
                notifyDisconnected(ConnectionStatus.AuthFailed)
            } catch (secEx: SecurityException) {
                failedConnectAttempts++
                authFailedPause = false
                Log.w(TAG, "Нарушение безопасности канала или нет прав Bluetooth")
                notifyDisconnected(ConnectionStatus.SecurityError)
            } catch (e: Exception) {
                failedConnectAttempts++
                authFailedPause = false
                Log.w(TAG, "Связь с Т2 оборвалась или сбой подключения (попытка #$failedConnectAttempts)")
                notifyDisconnected(ConnectionStatus.Searching)
            } finally {
                keepaliveFuture?.cancel(false)
                isConnected = false
                activeChannel = null
                failAllInFlightSmsOnDisconnect()
                abortPendingServerContactsRequest()
                releasePulseWakeLock(force = false)
                try { currentSocket?.close() } catch (_: Exception) {}
                try { activeSocket?.close() } catch (_: Exception) {}
                activeSocket = null
                if (isRunning && !skipFinallyWait) {
                    val delayMs = getReconnectDelayMs(failedConnectAttempts)
                    waitBeforeReconnect(delayMs)
                }
            }
        }
    }

    private fun handleIncomingFrameWithWakeLock(frameText: String) {
        val json = JSONObject(frameText)
        val type = json.optString("t", "")
        val isHighPriorityEvent = (type == "CALL_RINGING" || type == "SMS_RECV" || type == "MISSED_CALL")

        if (isHighPriorityEvent) {
            acquirePulseWakeLock(4000L, lockFloor = true)
            handleIncomingJson(json)
        } else {
            acquirePulseWakeLock(1500L, lockFloor = false)
            try {
                handleIncomingJson(json)
            } finally {
                releasePulseWakeLock(force = false)
            }
        }
    }

    private inline fun notifyUi(crossinline action: (ServiceCallback) -> Unit) {
        val cb = callback
        if (cb != null) {
            mainHandler.post {
                callback?.let { action(it) }
            }
        }
    }

    private fun isDuplicateEventId(eventId: String): Boolean {
        if (eventId.isEmpty()) return false
        val now = System.currentTimeMillis()
        synchronized(seenEventsLock) {
            val iter = seenEvents.entries.iterator()
            while (iter.hasNext()) {
                if (iter.next().value.expiresAtMs <= now) {
                    iter.remove()
                }
            }

            val existing = seenEvents[eventId]
            if (existing != null && existing.expiresAtMs > now) {
                return true
            }

            if (seenEvents.size >= MAX_SEEN_EVENTS) {
                val oldestKey = seenEvents.keys.firstOrNull()
                if (oldestKey != null) {
                    seenEvents.remove(oldestKey)
                }
            }
            seenEvents[eventId] = SeenEntry(now + EVENT_DEDUP_TTL_MS)
            return false
        }
    }

    private fun sendEventAck(eventId: String) {
        if (eventId.isEmpty()) return
        sendJson(JSONObject().apply {
            put("t", "EVENT_ACK")
            put("eventId", eventId)
        })
    }

    private fun handleIncomingJson(json: JSONObject) {
        when (json.optString("t", "")) {
            "PONG" -> {
                // Активность и сброс missedPongs уже обновлены при чтении кадра
            }

            "STATUS_UPDATE" -> {
                lastStatusReceivedTime = System.currentTimeMillis()
                val batteryPct = json.optInt("battery", -1)
                val isCharging = json.optBoolean("charging", false)
                val operator = json.optString("operator", "Неизвестно")
                val signalLevel = json.optInt("signal", 0)
                val dataStatus = json.optString("data", "Неизвестно")
                val callRinging = json.optBoolean("callRinging", false)
                val callerNumber = json.optString("callerNumber", "")
                val callerName = json.optString("callerName", "")
                val (hsState, hsPending, hsRequested) = hotspotTracker.applyStatus(json.optInt("hotspot", -1))
                val (mdState, mdPending, mdRequested) = mobileDataTracker.applyStatus(json.optInt("mobileData", -1))

                lastBatteryPct = batteryPct
                lastIsCharging = isCharging
                lastOperator = operator
                lastSignalLevel = signalLevel
                lastDataStatus = dataStatus

                if (connectionStatus == ConnectionStatus.Refreshing) {
                    connectionStatus = ConnectionStatus.Connected
                    lastStatusText = formatConnectionStatus(connectionStatus)
                }

                postOnlineStatusNotification(force = false)

                notifyUi {
                    it.onStatusUpdated(batteryPct, isCharging, operator, signalLevel, dataStatus)
                    it.onHotspotStateChanged(hsState, hsPending, hsRequested)
                    it.onMobileDataStateChanged(mdState, mdPending, mdRequested)
                }

                if (callRinging && callerNumber.isNotEmpty()) {
                    val displayCaller = buildCallerDisplay(callerNumber, callerName)
                    if (!isCallActive || currentCallerRawNumber != callerNumber || currentCallerName != callerName || currentCallerDisplay != displayCaller) {
                        isCallActive = true
                        currentCallerRawNumber = callerNumber
                        currentCallerName = callerName
                        currentCallerDisplay = displayCaller
                        showCallNotification(displayCaller)
                        notifyUi { it.onIncomingCall(displayCaller) }
                    }
                } else if (!callRinging && isCallActive) {
                    isCallActive = false
                    currentCallerRawNumber = ""
                    currentCallerName = ""
                    currentCallerDisplay = ""
                    cancelCallNotification()
                    notifyUi { it.onCallEnded() }
                }
            }

            "CALL_RINGING" -> {
                val number = json.optString("number", "Скрытый номер").ifBlank { "Скрытый номер" }
                val name = json.optString("name", "")
                val displayCaller = buildCallerDisplay(number, name)

                isCallActive = true
                currentCallerRawNumber = number
                currentCallerName = name
                currentCallerDisplay = displayCaller

                showCallNotification(displayCaller)
                notifyUi { it.onIncomingCall(displayCaller) }
            }

            "CALL_ENDED" -> {
                isCallActive = false
                currentCallerRawNumber = ""
                currentCallerName = ""
                currentCallerDisplay = ""
                cancelCallNotification()
                notifyUi { it.onCallEnded() }
            }

            "SMS_RECV" -> {
                val eventId = json.optString("eventId", "")
                if (eventId.isNotEmpty()) {
                    sendEventAck(eventId)
                }

                val number = json.optString("number", "Неизвестный")
                val name = json.optString("name", "")
                val body = json.optString("body", "")
                val ts = json.optLong("ts", System.currentTimeMillis())
                val isSync = json.optBoolean("isSync", false)

                val effectiveEventId = eventId.ifEmpty { "sms_no_id:${ts}:${number.hashCode()}:${body.hashCode()}" }
                if (isDuplicateEventId(effectiveEventId) || repo().hasMessageEventId(effectiveEventId)) {
                    return
                }

                val isArchived = repo().isChatArchivedByAddress(number)
                val otpCode = OtpExtractor.extract(body, number)
                val isFreshSms = (System.currentTimeMillis() - ts) <= AUTO_COPY_MAX_AGE_MS
                val shouldAutoCopy = if (!isArchived && otpCode != null && !isSync && isFreshSms && isAutoCopyOtpEnabled(this)) {
                    OtpCopyReceiver.copyOtpToClipboard(this, otpCode, showToast = true)
                    true
                } else {
                    false
                }

                val (chatKey, wasArchived) = repo().recordIncomingSms(
                    eventId = effectiveEventId,
                    rawNumber = number,
                    senderName = name,
                    body = body,
                    timestampMs = ts,
                    otpCode = otpCode,
                    markRead = false,
                    isSync = isSync
                )

                val localizedSender = ServerMessageLocalizer.localizeSenderNumber(
                    number,
                    AppLocale.isEnglish(locContext())
                )
                val displaySender = if (name.isNotEmpty()) "$name ($localizedSender)" else localizedSender

                if (!wasArchived && repo().activeChatKey != chatKey) {
                    showSmsNotification(displaySender, body, ts, chatKey, otpCode, shouldAutoCopy)
                }
            }

            "MISSED_CALL" -> {
                val eventId = json.optString("eventId", "")
                if (eventId.isNotEmpty()) {
                    sendEventAck(eventId)
                }

                val number = json.optString("number", "Скрытый номер")
                val name = json.optString("name", "")
                val timestamp = json.optLong("ts", System.currentTimeMillis())

                val effectiveEventId = eventId.ifEmpty { "missed_no_id:${number}:$timestamp" }
                if (isDuplicateEventId(effectiveEventId) || repo().hasMissedCallEventId(effectiveEventId)) {
                    return
                }

                repo().recordMissedCall(effectiveEventId, number, name, timestamp)

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val formattedTime = timeFormat.format(Date(timestamp))
                val localizedCaller = ServerMessageLocalizer.localizeCallerNumber(
                    number,
                    AppLocale.isEnglish(locContext())
                )
                val displayCaller = if (name.isNotEmpty()) "$name ($localizedCaller)" else localizedCaller

                showMissedCallNotification(displayCaller, formattedTime)
                notifyUi { it.onStatusMessage(str(R.string.status_missed_call, displayCaller)) }
            }

            "SYNC_DONE" -> {
                val count = json.optInt("count", 0)
                val hasMore = json.optBoolean("hasMore", false)
                notifyUi { it.onSyncDone(count, hasMore) }
            }

            "SYNC_EMPTY" -> {
                notifyUi { it.onSyncEmpty() }
            }

            "CMD_ACK" -> {
                val cmdId = json.optString("id", "")
                val cmd = json.optString("cmd", "")
                val ok = json.optBoolean("ok", false)
                val rawMsg = json.optString("msg", "")
                val msg = ServerMessageLocalizer.localizeServerMessage(rawMsg, AppLocale.isEnglish(locContext()))
                val restored = if (!ok && cmd == "SEND_SMS") inFlightSms.remove(cmdId)?.body else null
                if (!ok && cmd == "SEND_SMS") {
                    repo().updateOutgoingMessageStatus(cmdId, ChatLogic.STATUS_FAILED)
                }
                if (!ok && cmd == "SET_HOTSPOT") {
                    hotspotTracker.onAckFailed()
                } else if (!ok && cmd == "SET_MOBILE_DATA") {
                    mobileDataTracker.onAckFailed()
                }
                notifyUi { it.onCommandResult(cmdId, cmd, ok, msg, restored) }
            }

            "SMS_SENT" -> {
                val cmdId = json.optString("id", "")
                val ok = json.optBoolean("ok", false)
                val rawMsg = json.optString("msg", "")
                val msg = ServerMessageLocalizer.localizeServerMessage(rawMsg, AppLocale.isEnglish(locContext()))
                val tracked = inFlightSms.remove(cmdId)
                repo().updateOutgoingMessageStatus(
                    cmdId,
                    if (ok) ChatLogic.STATUS_SENT else ChatLogic.STATUS_FAILED
                )
                val restored = if (!ok) tracked?.body else null
                notifyUi { it.onCommandResult(cmdId, "SMS_SENT", ok, msg, restored) }
            }

            "CONTACTS_CHUNK" -> {
                val reqId = json.optString("id", "")
                val items = json.optJSONArray("items")
                if (reqId.isNotEmpty() && items != null) {
                    synchronized(serverContactsLock) {
                        if (reqId == activeContactsReqId) {
                            for (i in 0 until items.length()) {
                                if (incomingContactsBuffer.size >= MAX_SERVER_CONTACTS) break
                                val item = items.optJSONObject(i) ?: continue
                                val rawName = item.optString("n", "").trim().take(120)
                                val cleanPhone = ContactsHelper.cleanDisplayPhone(item.optString("p", ""))
                                val canonical = ContactsHelper.canonicalPhoneKey(cleanPhone)
                                if (ContactsHelper.digitsOnly(canonical).length in 3..15) {
                                    incomingContactsBuffer.add(
                                        ContactsHelper.ContactEntry(
                                            name = rawName.ifEmpty { cleanPhone },
                                            phone = cleanPhone.ifEmpty { canonical },
                                            normalizedPhone = canonical
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "CONTACTS_END" -> {
                val reqId = json.optString("id", "")
                val ok = json.optBoolean("ok", false)
                val rawMsg = json.optString("msg", "").ifBlank { null }
                val msg = rawMsg?.let {
                    ServerMessageLocalizer.localizeServerMessage(it, AppLocale.isEnglish(locContext()))
                }
                var matchedRequest = false
                var resultContacts: List<ContactsHelper.ContactEntry> = emptyList()
                synchronized(serverContactsLock) {
                    if (reqId.isNotEmpty() && reqId == activeContactsReqId) {
                        matchedRequest = true
                        activeContactsReqId = null
                        if (ok) {
                            val sorted = ContactsHelper.sortContactsEnThenRu(
                                ContactsHelper.deduplicateContacts(incomingContactsBuffer)
                            )
                            cachedServerContacts = sorted
                            hasLoadedServerContacts = true
                            lastServerContactsLoadedAtMs = System.currentTimeMillis()
                            resultContacts = sorted
                        } else {
                            resultContacts = cachedServerContacts
                        }
                        incomingContactsBuffer.clear()
                    }
                }
                if (matchedRequest) {
                    notifyUi { it.onServerContactsLoaded(ok, resultContacts, msg) }
                }
            }
        }
    }

    private fun markInFlightSmsUnknown(cmdId: String) {
        if (inFlightSms.remove(cmdId) != null) {
            repo().updateOutgoingMessageStatus(cmdId, ChatLogic.STATUS_UNKNOWN)
            val msg = str(R.string.sms_status_unknown_disconnect)
            notifyUi {
                it.onCommandResult(
                    cmdId,
                    "SMS_UNKNOWN",
                    false,
                    msg,
                    null
                )
            }
        }
    }

    private fun failAllInFlightSmsOnDisconnect() {
        val keys = inFlightSms.keys.toList()
        for (cmdId in keys) {
            markInFlightSmsUnknown(cmdId)
        }
    }

    private fun notifyConnected(status: ConnectionStatus) {
        isConnected = true
        connectionStatus = status
        val message = formatConnectionStatus(status)
        lastStatusText = message
        notifyUi { it.onConnectionStateChanged(true, message) }
    }

    private fun abortPendingServerContactsRequest() {
        var abortedContactsReq = false
        var cachedContacts: List<ContactsHelper.ContactEntry> = emptyList()
        synchronized(serverContactsLock) {
            if (activeContactsReqId != null) {
                abortedContactsReq = true
                activeContactsReqId = null
                incomingContactsBuffer.clear()
                cachedContacts = cachedServerContacts
            }
        }
        if (abortedContactsReq) {
            val msg = str(R.string.status_connection_lost)
            notifyUi { it.onServerContactsLoaded(false, cachedContacts, msg) }
        }
    }

    private fun notifyDisconnected(status: ConnectionStatus) {
        isConnected = false
        connectionStatus = status
        val message = formatConnectionStatus(status)
        lastStatusText = message

        failAllInFlightSmsOnDisconnect()
        abortPendingServerContactsRequest()
        hotspotTracker.reset()
        mobileDataTracker.reset()

        if (isCallActive) {
            isCallActive = false
            currentCallerRawNumber = ""
            currentCallerName = ""
            currentCallerDisplay = ""
            cancelCallNotification()
            notifyUi { it.onCallEnded() }
        }

        updateStatusNotification(
            title = str(R.string.notif_title_searching),
            contentText = "🔋 --%   📶 --   🌐 --",
            detailText = message
        )

        notifyUi { it.onConnectionStateChanged(false, message) }
    }

    private fun sendJson(json: JSONObject, onWriteFailed: (() -> Unit)? = null): Boolean {
        if (!isRunning || sendExecutor.isShutdown) return false
        val ch = activeChannel ?: return false
        val payload = json.toString()
        sendExecutor.execute {
            try {
                ch.sendText(payload)
            } catch (e: Exception) {
                Log.w(TAG, "Ошибка отправки команды на Т2")
                onWriteFailed?.invoke()
                try { activeSocket?.close() } catch (_: Exception) {}
            }
        }
        return true
    }

    private fun flushPendingSmsQueue() {
        val now = System.currentTimeMillis()
        val batch = mutableListOf<QueuedSms>()
        val expiredCmdIds = mutableListOf<String>()

        synchronized(queueLock) {
            while (pendingSmsQueue.isNotEmpty()) {
                val sms = pendingSmsQueue.removeFirst()
                if (now - sms.createdAtMs > PENDING_SMS_TTL_MS) {
                    expiredCmdIds.add(sms.cmdId)
                } else {
                    batch.add(sms)
                }
            }
            persistPendingSmsQueueLocked()
        }

        for (expiredId in expiredCmdIds) {
            repo().updateOutgoingMessageStatus(expiredId, ChatLogic.STATUS_FAILED)
        }

        if (expiredCmdIds.isNotEmpty()) {
            val expiredMsg = str(R.string.sms_expired_cancelled, expiredCmdIds.size)
            notifyUi {
                it.onStatusMessage(expiredMsg)
            }
        }

        for (sms in batch) {
            inFlightSms[sms.cmdId] = sms
            val sent = sendJson(JSONObject().apply {
                put("t", "SEND_SMS")
                put("id", sms.cmdId)
                put("number", sms.number)
                put("body", sms.body)
            }) {
                markInFlightSmsUnknown(sms.cmdId)
            }
            if (sent) {
                repo().updateOutgoingMessageStatus(sms.cmdId, ChatLogic.STATUS_SENDING)
            } else {
                inFlightSms.remove(sms.cmdId)
                synchronized(queueLock) {
                    if (pendingSmsQueue.size < MAX_QUEUED_SMS) {
                        pendingSmsQueue.addLast(sms)
                        persistPendingSmsQueueLocked()
                    }
                }
            }
        }
    }

    fun refreshStatus() {
        acquirePulseWakeLock(3000L, lockFloor = false)
        ensureStatusNotificationVisible()
        if (isConnected && activeChannel != null) {
            sendJson(JSONObject().apply {
                put("t", "GET_STATUS")
                put("id", nextCmdId())
            })
            connectionStatus = ConnectionStatus.Refreshing
            val msg = formatConnectionStatus(connectionStatus)
            lastStatusText = msg
            notifyUi { it.onStatusMessage(msg) }
        } else {
            connectionStatus = ConnectionStatus.SearchingAndConnecting
            val msg = formatConnectionStatus(connectionStatus)
            lastStatusText = msg
            notifyUi { it.onStatusMessage(msg) }
            triggerImmediateReconnect()
        }
    }

    fun onUiOpened() {
        ensureStatusNotificationVisible()
        if (!isConnected && hasServerConfig() && bluetoothAdapter?.isEnabled == true) {
            triggerImmediateReconnect()
        } else if (isConnected && System.currentTimeMillis() - lastStatusReceivedTime > 15_000L) {
            sendJson(JSONObject().apply {
                put("t", "GET_STATUS")
                put("id", nextCmdId())
            })
        }
    }

    fun onLanguageChanged() {
        localizedContext = AppLocale.wrap(this)
        createNotificationChannels()

        val statusMsg = formatConnectionStatus(connectionStatus)
        lastStatusText = statusMsg

        when {
            isConnected && lastStatusReceivedTime > 0L -> postOnlineStatusNotification(force = true)
            isConnected -> updateStatusNotification(
                title = str(R.string.notif_title_online),
                contentText = "🔋 --%   📶 --   🌐 --",
                detailText = statusMsg,
                force = true
            )
            !hasServerConfig() -> updateStatusNotification(
                title = str(R.string.notif_title_setup_required),
                contentText = "🔋 --%   📶 --   🌐 --",
                detailText = str(R.string.notif_detail_setup_required),
                force = true
            )
            connectionStatus == ConnectionStatus.NoBtPermission -> updateStatusNotification(
                title = str(R.string.notif_title_permission_required),
                contentText = "🔋 --%   📶 --   🌐 --",
                detailText = str(R.string.notif_detail_grant_bt),
                force = true
            )
            connectionStatus == ConnectionStatus.Preparing -> updateStatusNotification(
                title = str(R.string.notif_title_waiting),
                contentText = "🔋 --%   📶 --   🌐 --",
                detailText = str(R.string.status_waiting_connection),
                force = true
            )
            else -> updateStatusNotification(
                title = str(R.string.notif_title_searching),
                contentText = "🔋 --%   📶 --   🌐 --",
                detailText = statusMsg,
                force = true
            )
        }

        if (isCallActive && currentCallerRawNumber.isNotEmpty()) {
            currentCallerDisplay = buildCallerDisplay(currentCallerRawNumber, currentCallerName)
            showCallNotification(currentCallerDisplay)
        }

        notifyUi { it.onStatusMessage(statusMsg) }
    }

    fun setHotspotEnabled(enabled: Boolean): Pair<Boolean, String> =
        hotspotTracker.dispatch(enabled)

    fun setMobileDataEnabled(enabled: Boolean): Pair<Boolean, String> =
        mobileDataTracker.dispatch(enabled)

    fun sendDial(rawNumber: String): Pair<Boolean, String> {
        val cleanNumber = BtProtocol.sanitizeAndValidatePhone(rawNumber)
            ?: return Pair(false, str(R.string.error_invalid_phone))

        if (!isConnected || activeChannel == null) {
            return Pair(false, str(R.string.dial_no_connection))
        }

        val cmdId = nextCmdId()
        val dispatched = sendJson(JSONObject().apply {
            put("t", "DIAL")
            put("id", cmdId)
            put("number", cleanNumber)
        })
        return if (dispatched) {
            Pair(true, str(R.string.dial_sending))
        } else {
            Pair(false, str(R.string.error_no_active_channel))
        }
    }

    fun getCachedServerContacts(): List<ContactsHelper.ContactEntry> {
        synchronized(serverContactsLock) {
            return cachedServerContacts
        }
    }

    fun hasCachedServerContacts(): Boolean {
        synchronized(serverContactsLock) {
            return hasLoadedServerContacts
        }
    }

    fun getServerContactsLoadedAtMs(): Long {
        synchronized(serverContactsLock) {
            return lastServerContactsLoadedAtMs
        }
    }

    fun requestServerContacts(): Pair<Boolean, String> {
        if (!isConnected || activeChannel == null) {
            return Pair(false, str(R.string.contacts_no_connection))
        }
        val cmdId = nextCmdId()
        synchronized(serverContactsLock) {
            activeContactsReqId = cmdId
            incomingContactsBuffer.clear()
        }
        val dispatched = sendJson(JSONObject().apply {
            put("t", "GET_CONTACTS")
            put("id", cmdId)
        })
        if (!dispatched) {
            synchronized(serverContactsLock) {
                if (activeContactsReqId == cmdId) {
                    activeContactsReqId = null
                    incomingContactsBuffer.clear()
                }
            }
            return Pair(false, str(R.string.error_no_active_channel))
        }
        return Pair(true, str(R.string.contacts_loading_server))
    }

    fun sendSms(rawNumber: String, text: String, displayName: String? = null): Pair<Boolean, String> {
        val res = sendSmsDetailed(rawNumber, text, displayName)
        return Pair(res.ok, res.message)
    }

    fun sendSmsDetailed(rawNumber: String, text: String, displayName: String? = null): SmsDispatchResult {
        val cleanNumber = BtProtocol.sanitizeAndValidatePhone(rawNumber)
            ?: return SmsDispatchResult(false, str(R.string.error_invalid_phone))

        if (text.isBlank()) {
            return SmsDispatchResult(false, str(R.string.error_empty_sms_text))
        }
        if (text.length > MAX_CLIENT_SMS_CHARS) {
            return SmsDispatchResult(false, str(R.string.error_sms_too_long, MAX_CLIENT_SMS_CHARS))
        }

        val shortCodeHint = if (cleanNumber.length <= 5 && !cleanNumber.startsWith("+")) {
            str(R.string.sms_short_code_hint)
        } else {
            ""
        }

        val cmdId = nextCmdId()
        val sms = QueuedSms(cmdId, cleanNumber, text, System.currentTimeMillis())

        var recordedChatKey: String? = null
        if (isConnected && activeChannel != null) {
            recordedChatKey = repo().recordOutgoingSms(
                cmdId = cmdId,
                rawNumber = cleanNumber,
                displayName = displayName,
                body = text,
                timestampMs = sms.createdAtMs,
                initialStatus = ChatLogic.STATUS_SENDING
            )
            inFlightSms[cmdId] = sms
            val dispatched = sendJson(JSONObject().apply {
                put("t", "SEND_SMS")
                put("id", cmdId)
                put("number", cleanNumber)
                put("body", text)
            }) {
                markInFlightSmsUnknown(cmdId)
            }
            if (dispatched) {
                recordRecentSmsContact(this, rawNumber, displayName)
                return SmsDispatchResult(true, str(R.string.sms_sending_to_server, shortCodeHint), recordedChatKey, cmdId)
            }
            inFlightSms.remove(cmdId)
        }

        val queuedCount = synchronized(queueLock) {
            if (pendingSmsQueue.size >= MAX_QUEUED_SMS) {
                -1
            } else {
                pendingSmsQueue.addLast(sms)
                persistPendingSmsQueueLocked()
                pendingSmsQueue.size
            }
        }
        if (queuedCount < 0) {
            if (recordedChatKey != null) {
                repo().updateOutgoingMessageStatus(cmdId, ChatLogic.STATUS_FAILED)
            }
            return SmsDispatchResult(false, str(R.string.sms_queue_full))
        }
        val finalChatKey = if (recordedChatKey != null) {
            repo().updateOutgoingMessageStatus(cmdId, ChatLogic.STATUS_QUEUED)
            recordedChatKey
        } else {
            repo().recordOutgoingSms(
                cmdId = cmdId,
                rawNumber = cleanNumber,
                displayName = displayName,
                body = text,
                timestampMs = sms.createdAtMs,
                initialStatus = ChatLogic.STATUS_QUEUED
            )
        }
        recordRecentSmsContact(this, rawNumber, displayName)
        return SmsDispatchResult(
            true,
            str(R.string.sms_queued_offline, queuedCount, shortCodeHint),
            finalChatKey,
            cmdId
        )
    }

    fun requestSync5Min(): Pair<Boolean, String> {
        if (!isConnected || activeChannel == null) {
            return Pair(false, str(R.string.sync_no_connection))
        }
        val dispatched = sendJson(JSONObject().apply {
            put("t", "SYNC_5MIN")
            put("id", nextCmdId())
        })
        return if (dispatched) {
            Pair(true, str(R.string.sync_5min_requested))
        } else {
            Pair(false, str(R.string.error_no_active_channel))
        }
    }

    fun requestSyncDisconnect(): Pair<Boolean, String> {
        if (!isConnected || activeChannel == null) {
            return Pair(false, str(R.string.sync_no_connection))
        }
        val dispatched = sendJson(JSONObject().apply {
            put("t", "SYNC_DISCONNECT")
            put("id", nextCmdId())
        })
        return if (dispatched) {
            Pair(true, str(R.string.sync_disconnect_requested))
        } else {
            Pair(false, str(R.string.error_no_active_channel))
        }
    }

    fun declineCall() {
        if (isConnected && activeChannel != null) {
            sendJson(JSONObject().apply {
                put("t", "DECLINE")
                put("id", nextCmdId())
            })
        }
        isCallActive = false
        currentCallerRawNumber = ""
        currentCallerName = ""
        currentCallerDisplay = ""
        cancelCallNotification()
        notifyUi { it.onCallEnded() }
    }

    private fun postOnlineStatusNotification(force: Boolean) {
        val isEng = AppLocale.isEnglish(locContext())
        val batteryPct = lastBatteryPct
        val isCharging = lastIsCharging
        val operator = ServerMessageLocalizer.localizeOperator(lastOperator, isEng)
        val signalLevel = lastSignalLevel
        val dataStatus = ServerMessageLocalizer.localizeDataStatus(lastDataStatus, isEng)

        val battIcon = if (isCharging) "⚡" else if (batteryPct <= 20) "🪫" else "🔋"
        val battStr = if (batteryPct >= 0) "$battIcon $batteryPct%" else "🔋 --%"

        val bars = when (signalLevel) {
            1 -> "[●○○○]"
            2 -> "[●●○○]"
            3 -> "[●●●○]"
            4 -> "[●●●●]"
            else -> "[○○○○]"
        }
        val netStr = "📶 $operator $bars"
        val dataStr = "🌐 $dataStatus"

        val summary = "$battStr   $netStr   $dataStr"
        val details = str(R.string.notif_detail_online, battStr, operator, signalLevel, dataStatus)

        updateStatusNotification(
            title = str(R.string.notif_title_online),
            contentText = summary,
            detailText = details,
            force = force
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val statusChannel = NotificationChannel(
                STATUS_CHANNEL_ID, str(R.string.channel_status_name), NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = str(R.string.channel_status_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(statusChannel)

            val channel = NotificationChannel(
                CALL_CHANNEL_ID, str(R.string.channel_call_name), NotificationManager.IMPORTANCE_HIGH
            ).apply { description = str(R.string.channel_call_desc) }
            nm.createNotificationChannel(channel)

            val smsChannel = NotificationChannel(
                SMS_CHANNEL_ID, str(R.string.channel_sms_name), NotificationManager.IMPORTANCE_HIGH
            ).apply { description = str(R.string.channel_sms_desc) }
            nm.createNotificationChannel(smsChannel)

            val missedChannel = NotificationChannel(
                MISSED_CHANNEL_ID, str(R.string.channel_missed_name), NotificationManager.IMPORTANCE_HIGH
            ).apply { description = str(R.string.channel_missed_desc) }
            nm.createNotificationChannel(missedChannel)
        }
    }

    private fun buildStatusNotification(title: String, contentText: String, detailText: String? = null): Notification {
        val openActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val refreshIntent = Intent(this, BTRouterClientService::class.java).apply {
            action = ACTION_REFRESH_STATUS
        }
        val refreshPendingIntent = PendingIntent.getService(
            this,
            1,
            refreshIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val restoreIntent = Intent(this, BTRouterClientService::class.java).apply {
            action = ACTION_RESTORE_STATUS_NOTIFICATION
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            3,
            restoreIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_popup_sync, str(R.string.notif_action_refresh), refreshPendingIntent)

        if (!detailText.isNullOrEmpty()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
        }

        return builder.build()
    }

    private fun isStatusNotificationActive(): Boolean {
        return try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.activeNotifications.any { it.id == STATUS_NOTIFICATION_ID }
        } catch (_: Exception) {
            true
        }
    }

    private fun postStatusNotification(notification: Notification, wasMissing: Boolean) {
        if (wasMissing) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        STATUS_NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    )
                } else {
                    startForeground(STATUS_NOTIFICATION_ID, notification)
                }
            } catch (_: Exception) {
                Log.w(TAG, "Не удалось повторно привязать FGS-уведомление, отправляю через notify()")
            }
        }
        try {
            NotificationManagerCompat.from(this).notify(STATUS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun postNotificationSafely(notificationId: Int, notification: Notification) {
        if (!CompatUtils.hasPostNotificationsPermission(this)) return
        try {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Нет разрешения POST_NOTIFICATIONS для уведомления $notificationId", e)
        }
    }

    @Synchronized
    fun ensureStatusNotificationVisible(flushIfPending: Boolean = false) {
        if (!isInitialized) return
        if (!CompatUtils.hasPostNotificationsPermission(this)) {
            hasPendingNotificationUpdate = true
            return
        }
        if ((flushIfPending && hasPendingNotificationUpdate) || !isStatusNotificationActive()) {
            updateStatusNotification(
                title = pendingNotifTitle,
                contentText = pendingNotifContent,
                detailText = pendingNotifDetail,
                force = true
            )
        }
    }

    @Synchronized
    private fun updateStatusNotification(
        title: String,
        contentText: String,
        detailText: String? = null,
        force: Boolean = false
    ) {
        val resolvedDetail = detailText ?: ""
        pendingNotifTitle = title
        pendingNotifContent = contentText
        pendingNotifDetail = resolvedDetail

        if (!isScreenOn && !force) {
            hasPendingNotificationUpdate = true
            return
        }

        val isMissing = !isStatusNotificationActive()

        if (!force &&
            !isMissing &&
            title == lastPostedNotifTitle &&
            contentText == lastPostedNotifContent &&
            resolvedDetail == lastPostedNotifDetail
        ) {
            hasPendingNotificationUpdate = false
            return
        }

        if (!CompatUtils.hasPostNotificationsPermission(this)) {
            hasPendingNotificationUpdate = true
            return
        }

        val notification = buildStatusNotification(title, contentText, resolvedDetail.ifEmpty { null })
        postStatusNotification(notification, wasMissing = isMissing)

        lastPostedNotifTitle = title
        lastPostedNotifContent = contentText
        lastPostedNotifDetail = resolvedDetail
        hasPendingNotificationUpdate = false
    }

    private fun showCallNotification(phoneNumber: String) {
        if (!CompatUtils.hasPostNotificationsPermission(this)) return

        val openActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val declineIntent = Intent(this, BTRouterClientService::class.java).apply {
            action = ACTION_DECLINE_CALL
        }
        val declinePendingIntent = PendingIntent.getService(
            this,
            2,
            declineIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val publicVersion = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(str(R.string.incoming_call_title))
            .setContentText(str(R.string.notif_call_hidden))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        val builder = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(str(R.string.incoming_call_title))
            .setContentText(phoneNumber)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, str(R.string.btn_decline_call), declinePendingIntent)

        postNotificationSafely(CALL_NOTIFICATION_ID, builder.build())
    }

    private fun cancelCallNotification() {
        NotificationManagerCompat.from(this).cancel(CALL_NOTIFICATION_ID)
    }

    private fun showSmsNotification(
        sender: String,
        messageBody: String,
        timestampMs: Long,
        chatKey: String,
        otpCode: String? = null,
        autoCopied: Boolean = false
    ) {
        if (!CompatUtils.hasPostNotificationsPermission(this)) return

        val notifId = dynamicNotifIdGen.getAndIncrement()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHAT_KEY, chatKey)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notifId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val publicVersion = NotificationCompat.Builder(this, SMS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(str(R.string.notif_sms_public_title))
            .setContentText(str(R.string.notif_sms_hidden))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setWhen(timestampMs)
            .build()

        val contentTitle = when {
            otpCode != null && autoCopied -> str(R.string.notif_sms_otp_copied, otpCode, sender)
            otpCode != null -> str(R.string.notif_sms_otp, otpCode, sender)
            else -> str(R.string.notif_sms_from, sender)
        }

        val builder = NotificationCompat.Builder(this, SMS_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(contentTitle)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setWhen(timestampMs)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (otpCode != null) {
            val copyIntent = Intent(this, OtpCopyReceiver::class.java).apply {
                action = ACTION_COPY_OTP
                putExtra(EXTRA_OTP_CODE, otpCode)
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                this,
                notifId,
                copyIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                android.R.drawable.ic_menu_edit,
                str(R.string.notif_action_copy_otp, otpCode),
                copyPendingIntent
            )
        }

        postNotificationSafely(notifId, builder.build())
    }

    private fun showMissedCallNotification(caller: String, timeFormatted: String) {
        if (!CompatUtils.hasPostNotificationsPermission(this)) return

        val notifId = dynamicNotifIdGen.getAndIncrement()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MISSED_CALLS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notifId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val publicVersion = NotificationCompat.Builder(this, MISSED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle(str(R.string.notif_missed_title))
            .setContentText(str(R.string.notif_missed_hidden))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .build()

        val builder = NotificationCompat.Builder(this, MISSED_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle(str(R.string.notif_missed_title))
            .setContentText(str(R.string.notif_missed_short, caller, timeFormatted))
            .setStyle(NotificationCompat.BigTextStyle().bigText(str(R.string.notif_missed_big, caller, timeFormatted)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        postNotificationSafely(notifId, builder.build())
    }

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                when (state) {
                    BluetoothAdapter.STATE_ON -> {
                        Log.d(TAG, "Bluetooth на Т1 включён — немедленный старт подключения")
                        triggerImmediateReconnect()
                    }
                    BluetoothAdapter.STATE_TURNING_OFF, BluetoothAdapter.STATE_OFF -> {
                        notifyDisconnected(ConnectionStatus.BtDisabled)
                        try { activeSocket?.close() } catch (_: Exception) {}
                        activeSocket = null
                        activeChannel = null
                    }
                }
            }
        }
    }

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    ensureStatusNotificationVisible(flushIfPending = true)
                    if (!isConnected && !authFailedPause && hasServerConfig() && bluetoothAdapter?.isEnabled == true) {
                        triggerImmediateReconnect()
                    } else if (isConnected && System.currentTimeMillis() - lastStatusReceivedTime > 60_000L) {
                        sendJson(JSONObject().apply {
                            put("t", "GET_STATUS")
                            put("id", nextCmdId())
                        })
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        restoreNotifRunnable?.let { mainHandler.removeCallbacks(it) }
        restoreNotifRunnable = null
        hotspotTracker.cancelTimer()
        mobileDataTracker.cancelTimer()

        synchronized(reconnectLock) {
            reconnectLock.notifyAll()
        }

        try { activeSocket?.close() } catch (_: Exception) {}
        activeSocket = null
        activeChannel = null

        sendExecutor.shutdownNow()
        scheduler.shutdownNow()

        if (isInitialized) {
            try { unregisterReceiver(btStateReceiver) } catch (_: Exception) {}
            try { unregisterReceiver(screenStateReceiver) } catch (_: Exception) {}
        }

        releasePulseWakeLock(force = true)
    }
}
