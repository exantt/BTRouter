package com.exantt.btrouterclient

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), BTRouterClientService.ServiceCallback, ChatRepository.Listener {

    private var clientService: BTRouterClientService? = null
    private var isBound = false
    private var appliedLanguage: String = ""
    private lateinit var chatRepo: ChatRepository

    private var statusRevertRunnable: Runnable? = null
    private var hasTransientStatus = false
    private var isWarningStatus = false

    // Индикаторы состояния сервера Т2
    private lateinit var mainRoot: FrameLayout
    private lateinit var mainContentColumn: LinearLayout
    private lateinit var layoutT2Status: LinearLayout
    private lateinit var btnRefreshStatus: Button
    private lateinit var btnConfigureT2: Button
    private lateinit var tvT2ConnIndicator: TextView
    private lateinit var tvT2Battery: TextView
    private lateinit var tvT2Network: TextView
    private lateinit var tvT2Data: TextView

    // Компактные круглые переключатели Mobile Data и Mobile Hotspot
    private lateinit var layoutQuickTogglesRow: LinearLayout
    private lateinit var layoutMobileDataTile: LinearLayout
    private lateinit var btnMobileDataTile: ImageView
    private lateinit var tvMobileDataLabel: TextView
    private lateinit var layoutHotspotTile: LinearLayout
    private lateinit var btnHotspotTile: ImageView
    private lateinit var tvHotspotLabel: TextView

    private lateinit var tvStatus: TextView
    private lateinit var callLayout: LinearLayout
    private lateinit var tvCallerNumber: TextView
    private lateinit var btnDecline: Button

    // Список чатов
    private lateinit var rvChats: RecyclerView
    private lateinit var layoutEmptyChats: LinearLayout
    private lateinit var chatAdapter: ChatListAdapter

    // Нижние кнопки и бейдж
    private lateinit var layoutMissedCallsFab: FrameLayout
    private lateinit var fabMissedCalls: ImageButton
    private lateinit var tvMissedCallsBadge: TextView
    private lateinit var fabNewSms: ImageButton
    private lateinit var tvAppVersion: TextView

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BTRouterClientService.LocalBinder
            val svc = localBinder.getService()
            clientService = svc
            svc.addCallback(this@MainActivity)
            isBound = true

            syncUIWithService()
            svc.onUiOpened()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            clientService?.removeCallback(this@MainActivity)
            clientService = null
            isBound = false
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        appliedLanguage = AppLocale.getLanguage(this)
        setContentView(R.layout.activity_main)

        chatRepo = ChatRepository.getInstance(this)

        mainRoot = findViewById(R.id.mainRoot)
        mainContentColumn = findViewById(R.id.mainContentColumn)
        layoutT2Status = findViewById(R.id.layoutT2Status)
        btnRefreshStatus = findViewById(R.id.btnRefreshStatus)
        btnConfigureT2 = findViewById(R.id.btnConfigureT2)
        tvT2ConnIndicator = findViewById(R.id.tvT2ConnIndicator)
        tvT2Battery = findViewById(R.id.tvT2Battery)
        tvT2Network = findViewById(R.id.tvT2Network)
        tvT2Data = findViewById(R.id.tvT2Data)

        layoutQuickTogglesRow = findViewById(R.id.layoutQuickTogglesRow)
        layoutMobileDataTile = findViewById(R.id.layoutMobileDataTile)
        btnMobileDataTile = findViewById(R.id.btnMobileDataTile)
        tvMobileDataLabel = findViewById(R.id.tvMobileDataLabel)
        layoutHotspotTile = findViewById(R.id.layoutHotspotTile)
        btnHotspotTile = findViewById(R.id.btnHotspotTile)
        tvHotspotLabel = findViewById(R.id.tvHotspotLabel)

        tvStatus = findViewById(R.id.tvStatus)
        callLayout = findViewById(R.id.callLayout)
        tvCallerNumber = findViewById(R.id.tvCallerNumber)
        btnDecline = findViewById(R.id.btnDecline)

        rvChats = findViewById(R.id.rvChats)
        layoutEmptyChats = findViewById(R.id.layoutEmptyChats)
        layoutMissedCallsFab = findViewById(R.id.layoutMissedCallsFab)
        fabMissedCalls = findViewById(R.id.fabMissedCalls)
        tvMissedCallsBadge = findViewById(R.id.tvMissedCallsBadge)
        fabNewSms = findViewById(R.id.fabNewSms)
        tvAppVersion = findViewById(R.id.tvAppVersion)

        applySystemInsets()
        setupVersionLabel()
        setupChatsList()
        setupQuickToggles()

        val refreshStatusAction = View.OnClickListener {
            clientService?.refreshStatus()
        }
        btnRefreshStatus.setOnClickListener(refreshStatusAction)
        layoutT2Status.setOnClickListener(refreshStatusAction)

        btnConfigureT2.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnDecline.setOnClickListener {
            clientService?.declineCall()
            callLayout.visibility = View.GONE
            showTransientStatus(getString(R.string.status_decline_sent))
        }

        fabNewSms.setOnClickListener {
            startActivity(Intent(this, NewSmsActivity::class.java))
        }

        fabMissedCalls.setOnClickListener {
            startActivity(Intent(this, MissedCallsActivity::class.java))
        }

        reloadChats()
        updateMissedCallsBadge()

        if (savedInstanceState == null) {
            requestNeededPermissions()
            handleNavigationIntent(intent)
        }
        if (!CompatUtils.hasBluetoothConnectPermission(this)) {
            setPersistentStatus(getString(R.string.status_grant_bt_permission))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(targetIntent: Intent?) {
        if (targetIntent == null) return
        val openChatKey = targetIntent.getStringExtra(BTRouterClientService.EXTRA_OPEN_CHAT_KEY)
        if (!openChatKey.isNullOrBlank()) {
            targetIntent.removeExtra(BTRouterClientService.EXTRA_OPEN_CHAT_KEY)
            if (chatRepo.getChat(openChatKey) != null) {
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CHAT_KEY, openChatKey)
                })
            }
            return
        }
        if (targetIntent.getBooleanExtra(BTRouterClientService.EXTRA_OPEN_MISSED_CALLS, false)) {
            targetIntent.removeExtra(BTRouterClientService.EXTRA_OPEN_MISSED_CALLS)
            startActivity(Intent(this, MissedCallsActivity::class.java))
        }
    }

    private fun setupChatsList() {
        chatAdapter = ChatListAdapter(
            onClick = { chat ->
                startActivity(Intent(this, ChatActivity::class.java).apply {
                    putExtra(ChatActivity.EXTRA_CHAT_KEY, chat.chatKey)
                })
            },
            onLongClick = { chat ->
                showChatContextMenu(chat)
            }
        )
        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = chatAdapter
    }

    private fun setupQuickToggles() {
        updateQuickTogglesVisibility()
        updateMobileDataUi()
        updateHotspotUi()

        layoutMobileDataTile.setOnClickListener {
            val svc = clientService
            if (svc == null) {
                showTransientStatus(getString(R.string.status_service_not_bound))
                return@setOnClickListener
            }
            val currentlyOn = isEffectiveToggleOn(
                svc.mobileDataPendingTarget,
                svc.lastMobileDataState,
                svc.lastRequestedMobileDataState
            )
            val result = svc.setMobileDataEnabled(!currentlyOn)
            showTransientStatus(result.second)
            if (result.first) {
                updateMobileDataUi()
            }
        }

        layoutHotspotTile.setOnClickListener {
            val svc = clientService
            if (svc == null) {
                showTransientStatus(getString(R.string.status_service_not_bound))
                return@setOnClickListener
            }
            val currentlyOn = isEffectiveToggleOn(
                svc.hotspotPendingTarget,
                svc.lastHotspotState,
                svc.lastRequestedHotspotState
            )
            val result = svc.setHotspotEnabled(!currentlyOn)
            showTransientStatus(result.second)
            if (result.first) {
                updateHotspotUi()
            }
        }
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(mainRoot) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val isRtl = ViewCompat.getLayoutDirection(mainRoot) == ViewCompat.LAYOUT_DIRECTION_RTL
            val startInset = if (isRtl) insets.right else insets.left
            val endInset = if (isRtl) insets.left else insets.right

            mainContentColumn.setPadding(
                dpToPx(12) + insets.left,
                dpToPx(10) + insets.top,
                dpToPx(12) + insets.right,
                0
            )
            rvChats.setPadding(
                rvChats.paddingLeft,
                rvChats.paddingTop,
                rvChats.paddingRight,
                dpToPx(88) + insets.bottom
            )

            (layoutMissedCallsFab.layoutParams as FrameLayout.LayoutParams).let { lp ->
                lp.marginStart = dpToPx(18) + startInset
                lp.bottomMargin = dpToPx(18) + insets.bottom
                layoutMissedCallsFab.layoutParams = lp
            }
            (fabNewSms.layoutParams as FrameLayout.LayoutParams).let { lp ->
                lp.marginEnd = dpToPx(18) + endInset
                lp.bottomMargin = dpToPx(18) + insets.bottom
                fabNewSms.layoutParams = lp
            }
            (tvAppVersion.layoutParams as FrameLayout.LayoutParams).let { lp ->
                lp.bottomMargin = dpToPx(10) + insets.bottom
                tvAppVersion.layoutParams = lp
            }
            windowInsets
        }
    }

    private fun requestNeededPermissions() {
        val candidates = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            candidates.add(Manifest.permission.BLUETOOTH_CONNECT)
            candidates.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            candidates.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = candidates.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        }
    }

    private fun startAndBindService() {
        if (!CompatUtils.hasBluetoothConnectPermission(this)) return
        val serviceIntent = Intent(this, BTRouterClientService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        if (!isBound) {
            isBound = bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStart() {
        super.onStart()
        if (AppLocale.getLanguage(this) != appliedLanguage) {
            recreate()
            return
        }
        chatRepo.addListener(this)
        updateQuickTogglesVisibility()
        reloadChats()
        updateMissedCallsBadge()

        val svc = clientService
        if (isBound && svc != null) {
            svc.addCallback(this)
            syncUIWithService()
            svc.onUiOpened()
        } else if (CompatUtils.hasBluetoothConnectPermission(this)) {
            startAndBindService()
        }
    }

    override fun onResume() {
        super.onResume()
        reloadChats()
        updateMissedCallsBadge()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (CompatUtils.hasBluetoothConnectPermission(this)) {
            startAndBindService()
            clientService?.onUiOpened()
        } else {
            setPersistentStatus(getString(R.string.status_bt_permission_required))
        }
    }

    override fun onStop() {
        super.onStop()
        chatRepo.removeListener(this)
        clientService?.removeCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelStatusRevertTimer()
        if (isBound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {
            }
            isBound = false
        }
    }

    // --- Реализация ChatRepository. Listener ---

    override fun onChatsChanged(chatKey: String?) {
        reloadChats()
    }

    override fun onMissedCallsChanged() {
        updateMissedCallsBadge()
    }

    private fun reloadChats() {
        val chats = chatRepo.getActiveChats()
        chatAdapter.submitList(chats)
        if (chats.isEmpty()) {
            rvChats.visibility = View.GONE
            layoutEmptyChats.visibility = View.VISIBLE
        } else {
            rvChats.visibility = View.VISIBLE
            layoutEmptyChats.visibility = View.GONE
        }
    }

    private fun updateMissedCallsBadge() {
        val lastViewedId = BTRouterClientService.getMissedCallsLastViewedId(this)
        val unreadMissed = if (lastViewedId >= 0L) {
            chatRepo.getUnreadMissedCallsCountAfterId(lastViewedId)
        } else {
            val lastViewedMs = BTRouterClientService.getMissedCallsLastViewedMs(this)
            chatRepo.getUnreadMissedCallsCount(lastViewedMs)
        }
        if (unreadMissed > 0) {
            tvMissedCallsBadge.text = if (unreadMissed > 99) "99+" else unreadMissed.toString()
            tvMissedCallsBadge.visibility = View.VISIBLE
        } else {
            tvMissedCallsBadge.visibility = View.GONE
        }
    }

    private fun showChatContextMenu(chat: ChatLogic.ChatThread) {
        val isEng = AppLocale.isEnglish(this)
        val title = if (chat.displayName.isNotBlank()) {
            chat.displayName
        } else {
            ServerMessageLocalizer.localizeSenderNumber(chat.address, isEng)
        }
        val pinLabel = getString(
            if (chat.isPinned) R.string.chat_action_unpin else R.string.chat_action_pin
        )
        val options = arrayOf(
            pinLabel,
            getString(R.string.chat_action_archive),
            getString(R.string.chat_action_delete)
        )
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val targetPin = !chat.isPinned
                        val ok = chatRepo.setPinned(chat.chatKey, targetPin)
                        if (!ok) {
                            Toast.makeText(this, getString(R.string.toast_pin_limit), Toast.LENGTH_SHORT).show()
                        } else {
                            val msgRes = if (targetPin) R.string.toast_chat_pinned else R.string.toast_chat_unpinned
                            Toast.makeText(this, getString(msgRes), Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        chatRepo.setArchived(chat.chatKey, true)
                        Toast.makeText(this, getString(R.string.toast_chat_archived), Toast.LENGTH_SHORT).show()
                    }
                    2 -> confirmDeleteChat(chat, title)
                }
            }
            .show()
    }

    private fun confirmDeleteChat(chat: ChatLogic.ChatThread, title: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_chat_title))
            .setMessage("$title\n\n${getString(R.string.dialog_delete_chat_message)}")
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                chatRepo.deleteChat(chat.chatKey)
                Toast.makeText(this, getString(R.string.toast_chat_deleted), Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun syncUIWithService() {
        val service = clientService ?: return

        if (!hasTransientStatus) {
            setPersistentStatus(service.lastStatusText)
        }
        updateQuickTogglesVisibility()

        if (service.isConnected) {
            updateT2StatusUI(
                service.lastBatteryPct,
                service.lastIsCharging,
                service.lastOperator,
                service.lastSignalLevel,
                service.lastDataStatus
            )
        } else {
            setT2DisconnectedUI()
        }
        updateMobileDataUi()
        updateHotspotUi()

        if (service.isCallActive) {
            tvCallerNumber.text = service.currentCallerDisplay
            callLayout.visibility = View.VISIBLE
        } else {
            callLayout.visibility = View.GONE
        }
    }

    // --- Реализация BTRouterClientService.ServiceCallback ---

    override fun onStatusUpdated(
        batteryPct: Int,
        isCharging: Boolean,
        operator: String,
        signalLevel: Int,
        dataStatus: String
    ) {
        updateT2StatusUI(batteryPct, isCharging, operator, signalLevel, dataStatus)
        val refreshingText = getString(R.string.status_refreshing_server)
        if (tvStatus.text == refreshingText) {
            showTransientStatus(getString(R.string.status_server_updated), 2000L)
        }
    }

    override fun onConnectionStateChanged(connected: Boolean, statusMessage: String) {
        setPersistentStatus(statusMessage)
        if (connected) {
            tvT2ConnIndicator.text = getString(R.string.main_conn_online)
            tvT2ConnIndicator.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            setT2DisconnectedUI()
        }
        updateMobileDataUi()
        updateHotspotUi()
    }

    override fun onHotspotStateChanged(hotspotState: Int, pendingTarget: Int, requestedState: Int) {
        updateHotspotUi()
        if (pendingTarget == -1 && hasTransientStatus) {
            shortenTransientStatusTo(1500L)
        }
    }

    override fun onMobileDataStateChanged(mobileDataState: Int, pendingTarget: Int, requestedState: Int) {
        updateMobileDataUi()
        if (pendingTarget == -1 && hasTransientStatus) {
            shortenTransientStatusTo(1500L)
        }
    }

    override fun onIncomingCall(callerDisplay: String) {
        tvCallerNumber.text = callerDisplay
        setPersistentStatus("")
        callLayout.visibility = View.VISIBLE
    }

    override fun onCallEnded() {
        callLayout.visibility = View.GONE
        if (clientService?.isConnected == true) {
            setPersistentStatus(clientService?.lastStatusText ?: getString(R.string.status_connected_encrypted))
        }
    }

    override fun onSyncDone(count: Int, hasMore: Boolean) {
        val syncText = if (hasMore) {
            getString(R.string.sync_done_more, count)
        } else {
            getString(R.string.sync_done_complete, count)
        }
        showTransientStatus(syncText, 4000L)
    }

    override fun onSyncEmpty() {
        showTransientStatus(getString(R.string.sync_empty), 4000L)
    }

    override fun onStatusMessage(message: String) {
        val persistentStatus = clientService?.lastStatusText
        if (clientService?.isConnected == true && message != persistentStatus) {
            showTransientStatus(message, 4000L)
        } else {
            setPersistentStatus(message)
        }
    }

    override fun onCommandResult(
        cmdId: String,
        cmd: String,
        ok: Boolean,
        message: String,
        restoredText: String?
    ) {
        val text = if (ok) message else getString(R.string.status_error_prefix, message)
        showTransientStatus(text, 3000L, isWarning = !ok)
    }

    override fun onToggleVerifyFailed(commandName: String, targetEnabled: Boolean, warningMessage: String) {
        showTransientStatus(warningMessage, durationMs = 6_000L, isWarning = true)
        Toast.makeText(this, warningMessage, Toast.LENGTH_LONG).show()
    }

    // --- Управление временными/постоянными сообщениями статуса (фикс микробага) ---

    private fun cancelStatusRevertTimer() {
        statusRevertRunnable?.let { tvStatus.removeCallbacks(it) }
        statusRevertRunnable = null
    }

    private fun setPersistentStatus(message: String) {
        cancelStatusRevertTimer()
        hasTransientStatus = false
        isWarningStatus = false
        tvStatus.setTextColor(Color.parseColor("#888888"))
        tvStatus.text = message
    }

    private fun showTransientStatus(message: String, durationMs: Long = 3000L, isWarning: Boolean = false) {
        cancelStatusRevertTimer()
        hasTransientStatus = true
        isWarningStatus = isWarning
        tvStatus.setTextColor(
            if (isWarning) Color.parseColor("#FFB74D") else Color.parseColor("#888888")
        )
        tvStatus.text = message
        val runnable = Runnable {
            statusRevertRunnable = null
            hasTransientStatus = false
            isWarningStatus = false
            if (isFinishing || isDestroyed) return@Runnable
            tvStatus.setTextColor(Color.parseColor("#888888"))
            if (clientService?.isCallActive == true) {
                tvStatus.text = ""
            } else {
                tvStatus.text = clientService?.lastStatusText ?: getString(R.string.status_waiting_short)
            }
        }
        statusRevertRunnable = runnable
        tvStatus.postDelayed(runnable, durationMs)
    }

    private fun shortenTransientStatusTo(delayMs: Long) {
        if (isWarningStatus) return
        val runnable = statusRevertRunnable ?: return
        tvStatus.removeCallbacks(runnable)
        tvStatus.postDelayed(runnable, delayMs)
    }

    // --- Статус Т2 и компактные круглые переключатели ---

    private fun updateT2StatusUI(
        batteryPct: Int,
        isCharging: Boolean,
        operator: String,
        signalLevel: Int,
        dataStatus: String
    ) {
        tvT2ConnIndicator.text = getString(R.string.main_conn_online)
        tvT2ConnIndicator.setTextColor(Color.parseColor("#4CAF50"))

        if (batteryPct >= 0) {
            val icon = if (isCharging) "⚡" else if (batteryPct <= 20) "🪫" else "🔋"
            tvT2Battery.text = "$icon $batteryPct%"
            val batteryColor = when {
                isCharging || batteryPct > 50 -> Color.parseColor("#4CAF50")
                batteryPct > 20 -> Color.parseColor("#FFC107")
                else -> Color.parseColor("#FF5252")
            }
            tvT2Battery.setTextColor(batteryColor)
        } else {
            tvT2Battery.text = "🔋 --%"
            tvT2Battery.setTextColor(Color.WHITE)
        }

        val bars = when (signalLevel) {
            1 -> "[●○○○]"
            2 -> "[●●○○]"
            3 -> "[●●●○]"
            4 -> "[●●●●]"
            else -> "[○○○○]"
        }
        val isEng = AppLocale.isEnglish(this)
        val localizedOperator = ServerMessageLocalizer.localizeOperator(operator, isEng)
        val localizedData = ServerMessageLocalizer.localizeDataStatus(dataStatus, isEng)

        tvT2Network.text = "📶 $localizedOperator $bars"
        tvT2Data.text = "🌐 $localizedData"
        val dataColor = if (ServerMessageLocalizer.isDataStatusOffline(dataStatus)) {
            Color.parseColor("#8E8E93")
        } else {
            Color.parseColor("#2196F3")
        }
        tvT2Data.setTextColor(dataColor)
    }

    private fun setT2DisconnectedUI() {
        tvT2ConnIndicator.text = getString(R.string.main_conn_searching)
        tvT2ConnIndicator.setTextColor(Color.parseColor("#FF453A"))
        tvT2Battery.text = "🔋 --%"
        tvT2Battery.setTextColor(Color.parseColor("#8E8E93"))
        tvT2Network.text = "📶 --"
        tvT2Network.setTextColor(Color.parseColor("#8E8E93"))
        tvT2Data.text = "🌐 --"
        tvT2Data.setTextColor(Color.parseColor("#8E8E93"))
    }

    private fun updateQuickTogglesVisibility() {
        val showMobileData = BTRouterClientService.isMobileDataControlVisible(this)
        val showHotspot = BTRouterClientService.isHotspotControlVisible(this)

        layoutMobileDataTile.visibility = if (showMobileData) View.VISIBLE else View.GONE
        layoutHotspotTile.visibility = if (showHotspot) View.VISIBLE else View.GONE

        (layoutHotspotTile.layoutParams as ViewGroup.MarginLayoutParams).let { lp ->
            lp.marginStart = if (showMobileData && showHotspot) dpToPx(28) else 0
            layoutHotspotTile.layoutParams = lp
        }

        layoutQuickTogglesRow.visibility = if (showMobileData || showHotspot) View.VISIBLE else View.GONE
    }

    private fun isEffectiveToggleOn(pending: Int, state: Int, requested: Int): Boolean {
        return when {
            pending == 1 -> true
            pending == 0 -> false
            state == 1 -> true
            state == 0 -> false
            else -> requested == 1
        }
    }

    private fun updateMobileDataUi() {
        val svc = clientService
        renderQuickTile(
            container = layoutMobileDataTile,
            circleView = btnMobileDataTile,
            labelView = tvMobileDataLabel,
            connected = svc != null && svc.isConnected,
            pending = svc?.mobileDataPendingTarget ?: -1,
            state = svc?.lastMobileDataState ?: -1,
            requested = svc?.lastRequestedMobileDataState ?: -1
        )
    }

    private fun updateHotspotUi() {
        val svc = clientService
        renderQuickTile(
            container = layoutHotspotTile,
            circleView = btnHotspotTile,
            labelView = tvHotspotLabel,
            connected = svc != null && svc.isConnected,
            pending = svc?.hotspotPendingTarget ?: -1,
            state = svc?.lastHotspotState ?: -1,
            requested = svc?.lastRequestedHotspotState ?: -1
        )
    }

    private fun renderQuickTile(
        container: LinearLayout,
        circleView: ImageView,
        labelView: TextView,
        connected: Boolean,
        pending: Int,
        state: Int,
        requested: Int
    ) {
        if (!connected) {
            container.isEnabled = false
            container.alpha = 0.45f
            circleView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#222224"))
            circleView.imageTintList = ColorStateList.valueOf(Color.parseColor("#8E8E93"))
            labelView.setTextColor(Color.parseColor("#666668"))
            return
        }

        container.isEnabled = true
        container.alpha = 1.0f

        when {
            pending == 1 || pending == 0 -> {
                circleView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B28704"))
                circleView.imageTintList = ColorStateList.valueOf(Color.WHITE)
                labelView.setTextColor(Color.parseColor("#FFD54F"))
            }
            state == 1 || (state == -1 && requested == 1) -> {
                circleView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2196F3"))
                circleView.imageTintList = ColorStateList.valueOf(Color.WHITE)
                labelView.setTextColor(Color.WHITE)
            }
            else -> {
                circleView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C2C2E"))
                circleView.imageTintList = ColorStateList.valueOf(Color.parseColor("#D1D1D6"))
                labelView.setTextColor(Color.parseColor("#9AA0A6"))
            }
        }
    }

    private fun setupVersionLabel() {
        val versionName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        } catch (_: Exception) {
            BuildConfig.VERSION_NAME
        }
        tvAppVersion.text = "v$versionName"
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
