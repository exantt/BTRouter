package com.exantt.btrouterclient

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity(), BTRouterClientService.ServiceCallback, ChatRepository.Listener {

    companion object {
        private const val STATE_SELECTED_MAC = "selected_server_mac"
    }

    private enum class BluetoothAction {
        NONE,
        REQUEST_PERMISSION,
        OPEN_SETTINGS
    }

    private var clientService: BTRouterClientService? = null
    private var isBound = false
    private var bondedDevicesList: List<BluetoothDevice> = emptyList()
    private var pendingBtAction = BluetoothAction.NONE
    private var restoredSelectedMac: String? = null
    private lateinit var chatRepo: ChatRepository

    private lateinit var settingsRoot: LinearLayout
    private lateinit var settingsScrollView: ScrollView
    private lateinit var layoutArchivedChatsCard: LinearLayout
    private lateinit var tvArchivedChatsSummary: TextView
    private lateinit var btnBack: Button
    private lateinit var btnRefreshDevices: Button
    private lateinit var tvDevicesNotice: TextView
    private lateinit var btnBluetoothAction: Button
    private lateinit var spinnerServerDevice: Spinner
    private lateinit var etPairingSecret: EditText
    private lateinit var cbShowSecret: CheckBox
    private lateinit var btnSaveGateway: Button
    private lateinit var tvGatewayStatus: TextView
    private lateinit var btnSyncSms: Button
    private lateinit var btnSyncDisconnect: Button
    private lateinit var tvSyncStatus: TextView
    private lateinit var switchAutoCopyOtp: SwitchCompat
    private lateinit var layoutMobileDataSettingInfo: LinearLayout
    private lateinit var btnMobileDataHelp: Button
    private lateinit var switchShowMobileData: SwitchCompat
    private lateinit var layoutHotspotSettingInfo: LinearLayout
    private lateinit var btnHotspotHelp: Button
    private lateinit var switchShowHotspot: SwitchCompat
    private lateinit var btnLangRu: Button
    private lateinit var btnLangEn: Button

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BTRouterClientService.LocalBinder
            clientService = localBinder.getService()
            clientService?.callback = this@SettingsActivity
            isBound = true
            tvGatewayStatus.text = clientService?.lastStatusText ?: getString(R.string.status_preparing)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            clientService = null
            isBound = false
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        restoredSelectedMac = savedInstanceState?.getString(STATE_SELECTED_MAC)
        chatRepo = ChatRepository.getInstance(this)

        settingsRoot = findViewById(R.id.settingsRoot)
        settingsScrollView = findViewById(R.id.settingsScrollView)
        layoutArchivedChatsCard = findViewById(R.id.layoutArchivedChatsCard)
        tvArchivedChatsSummary = findViewById(R.id.tvArchivedChatsSummary)
        btnBack = findViewById(R.id.btnBack)
        btnRefreshDevices = findViewById(R.id.btnRefreshDevices)
        tvDevicesNotice = findViewById(R.id.tvDevicesNotice)
        btnBluetoothAction = findViewById(R.id.btnBluetoothAction)
        spinnerServerDevice = findViewById(R.id.spinnerServerDevice)
        etPairingSecret = findViewById(R.id.etPairingSecret)
        cbShowSecret = findViewById(R.id.cbShowSecret)
        btnSaveGateway = findViewById(R.id.btnSaveGateway)
        tvGatewayStatus = findViewById(R.id.tvGatewayStatus)
        btnSyncSms = findViewById(R.id.btnSyncSms)
        btnSyncDisconnect = findViewById(R.id.btnSyncDisconnect)
        tvSyncStatus = findViewById(R.id.tvSyncStatus)
        switchAutoCopyOtp = findViewById(R.id.switchAutoCopyOtp)
        layoutMobileDataSettingInfo = findViewById(R.id.layoutMobileDataSettingInfo)
        btnMobileDataHelp = findViewById(R.id.btnMobileDataHelp)
        switchShowMobileData = findViewById(R.id.switchShowMobileData)
        layoutHotspotSettingInfo = findViewById(R.id.layoutHotspotSettingInfo)
        btnHotspotHelp = findViewById(R.id.btnHotspotHelp)
        switchShowHotspot = findViewById(R.id.switchShowHotspot)
        btnLangRu = findViewById(R.id.btnLangRu)
        btnLangEn = findViewById(R.id.btnLangEn)

        applySystemInsets()

        layoutArchivedChatsCard.setOnClickListener {
            startActivity(Intent(this, ArchivedChatsActivity::class.java))
        }
        updateArchivedSummary()

        btnBack.setOnClickListener { finish() }

        btnRefreshDevices.setOnClickListener {
            loadBondedDevices(preserveSelection = true)
        }

        btnBluetoothAction.setOnClickListener {
            when (pendingBtAction) {
                BluetoothAction.REQUEST_PERMISSION -> requestBluetoothPermissions()
                BluetoothAction.OPEN_SETTINGS -> openBluetoothSettings()
                BluetoothAction.NONE -> Unit
            }
        }

        if (savedInstanceState == null) {
            etPairingSecret.setText(BTRouterClientService.getSavedSecret(this) ?: "")
        }
        applySecretInputType(cbShowSecret.isChecked)
        cbShowSecret.setOnCheckedChangeListener { _, isChecked ->
            applySecretInputType(isChecked)
        }

        btnSaveGateway.setOnClickListener {
            saveGatewayConfig()
        }

        btnSyncSms.setOnClickListener {
            val result = clientService?.requestSync5Min()
            tvSyncStatus.text = result?.second ?: getString(R.string.status_service_not_bound)
            tvSyncStatus.visibility = View.VISIBLE
        }

        btnSyncDisconnect.setOnClickListener {
            val result = clientService?.requestSyncDisconnect()
            tvSyncStatus.text = result?.second ?: getString(R.string.status_service_not_bound)
            tvSyncStatus.visibility = View.VISIBLE
        }

        switchAutoCopyOtp.isChecked = BTRouterClientService.isAutoCopyOtpEnabled(this)
        switchAutoCopyOtp.setOnCheckedChangeListener { _, isChecked ->
            BTRouterClientService.setAutoCopyOtpEnabled(this, isChecked)
            Toast.makeText(
                this,
                if (isChecked) {
                    getString(R.string.toast_otp_auto_copy_on)
                } else {
                    getString(R.string.toast_otp_auto_copy_off)
                },
                Toast.LENGTH_SHORT
            ).show()
        }

        switchShowMobileData.isChecked = BTRouterClientService.isMobileDataControlVisible(this)
        switchShowMobileData.setOnCheckedChangeListener { _, isChecked ->
            BTRouterClientService.setMobileDataControlVisible(this, isChecked)
            Toast.makeText(
                this,
                if (isChecked) {
                    getString(R.string.toast_mobile_data_shown)
                } else {
                    getString(R.string.toast_mobile_data_hidden)
                },
                Toast.LENGTH_SHORT
            ).show()
        }

        val mobileDataGuideClickListener = View.OnClickListener { showMobileDataGuideDialog() }
        btnMobileDataHelp.setOnClickListener(mobileDataGuideClickListener)
        layoutMobileDataSettingInfo.setOnClickListener(mobileDataGuideClickListener)

        switchShowHotspot.isChecked = BTRouterClientService.isHotspotControlVisible(this)
        switchShowHotspot.setOnCheckedChangeListener { _, isChecked ->
            BTRouterClientService.setHotspotControlVisible(this, isChecked)
            Toast.makeText(
                this,
                if (isChecked) {
                    getString(R.string.toast_hotspot_shown)
                } else {
                    getString(R.string.toast_hotspot_hidden)
                },
                Toast.LENGTH_SHORT
            ).show()
        }

        val guideClickListener = View.OnClickListener { showHotspotGuideDialog() }
        btnHotspotHelp.setOnClickListener(guideClickListener)
        layoutHotspotSettingInfo.setOnClickListener(guideClickListener)

        updateLanguageButtonsUi()
        btnLangRu.setOnClickListener { selectLanguage(AppLocale.LANG_RU) }
        btnLangEn.setOnClickListener { selectLanguage(AppLocale.LANG_EN) }

        loadBondedDevices(preserveSelection = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val pos = spinnerServerDevice.selectedItemPosition
        if (pos in bondedDevicesList.indices) {
            outState.putString(STATE_SELECTED_MAC, bondedDevicesList[pos].address)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        applySecretInputType(cbShowSecret.isChecked)
    }

    override fun onStart() {
        super.onStart()
        chatRepo.addListener(this)
        updateArchivedSummary()
        if (isBound) {
            clientService?.callback = this
            tvGatewayStatus.text = clientService?.lastStatusText ?: getString(R.string.status_preparing)
        } else if (CompatUtils.hasBluetoothConnectPermission(this)) {
            bindClientService()
        } else {
            tvGatewayStatus.text = getString(R.string.status_grant_bt_permission)
        }
    }

    override fun onResume() {
        super.onResume()
        updateArchivedSummary()
        loadBondedDevices(preserveSelection = true)
    }

    override fun onStop() {
        super.onStop()
        chatRepo.removeListener(this)
        if (clientService?.callback === this) {
            clientService?.callback = null
        }
    }

    override fun onChatsChanged(chatKey: String?) {
        updateArchivedSummary()
    }

    private fun updateArchivedSummary() {
        val (chatCount, unreadCount) = chatRepo.getArchivedSummary()
        tvArchivedChatsSummary.text = when {
            chatCount == 0 -> getString(R.string.settings_archive_empty)
            unreadCount > 0 -> getString(R.string.settings_archive_summary_unread, chatCount, unreadCount)
            else -> getString(R.string.settings_archive_summary, chatCount)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        loadBondedDevices(preserveSelection = true)
        if (CompatUtils.hasBluetoothConnectPermission(this)) {
            startAndBindService()
        }
    }

    private fun applySystemInsets() {
        settingsScrollView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(settingsRoot) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeAndBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            settingsRoot.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            settingsScrollView.setPadding(0, 0, 0, imeAndBars.bottom)
            windowInsets
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 2)
        }
    }

    private fun applySecretInputType(visible: Boolean) {
        val hiddenType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val visibleType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        etPairingSecret.inputType = if (visible) visibleType else hiddenType
        etPairingSecret.typeface = Typeface.MONOSPACE
        etPairingSecret.setSelection(etPairingSecret.text?.length ?: 0)
    }

    private fun updateLanguageButtonsUi() {
        val activeBg = ColorStateList.valueOf(Color.parseColor("#2196F3"))
        val inactiveBg = ColorStateList.valueOf(Color.parseColor("#1C1C1E"))
        val activeText = Color.WHITE
        val inactiveText = Color.parseColor("#8E8E93")

        val isEn = AppLocale.isEnglish(this)
        btnLangRu.backgroundTintList = if (!isEn) activeBg else inactiveBg
        btnLangRu.setTextColor(if (!isEn) activeText else inactiveText)
        btnLangEn.backgroundTintList = if (isEn) activeBg else inactiveBg
        btnLangEn.setTextColor(if (isEn) activeText else inactiveText)
    }

    private fun selectLanguage(lang: String) {
        if (AppLocale.getLanguage(this) == lang) return
        AppLocale.setLanguage(this, lang)
        clientService?.onLanguageChanged()
        recreate()
    }

    private fun showMobileDataGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_mobile_data_guide_title)
            .setMessage(R.string.settings_mobile_data_guide_message)
            .setPositiveButton(R.string.btn_understood, null)
            .show()
    }

    private fun showHotspotGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.settings_hotspot_guide_title)
            .setMessage(R.string.settings_hotspot_guide_message)
            .setPositiveButton(R.string.btn_understood, null)
            .show()
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.toast_bt_settings_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindClientService() {
        val serviceIntent = Intent(this, BTRouterClientService::class.java)
        if (!isBound) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
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
        bindClientService()
    }

    private fun loadBondedDevices(preserveSelection: Boolean) {
        val prevMac = if (preserveSelection && spinnerServerDevice.selectedItemPosition in bondedDevicesList.indices) {
            bondedDevicesList[spinnerServerDevice.selectedItemPosition].address
        } else {
            null
        }

        if (!CompatUtils.hasBluetoothConnectPermission(this)) {
            showDeviceNotice(
                message = getString(R.string.settings_bt_perm_notice),
                buttonText = getString(R.string.btn_grant_bt_permission),
                action = BluetoothAction.REQUEST_PERMISSION
            )
            return
        }

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = btManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            showDeviceNotice(
                message = getString(R.string.settings_bt_off_notice),
                buttonText = getString(R.string.btn_open_bt_settings),
                action = BluetoothAction.OPEN_SETTINGS
            )
            return
        }

        val devices: List<BluetoothDevice> = try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        if (devices.isEmpty()) {
            showDeviceNotice(
                message = getString(R.string.settings_no_bonded_notice),
                buttonText = getString(R.string.btn_open_bt_settings),
                action = BluetoothAction.OPEN_SETTINGS
            )
            return
        }

        bondedDevicesList = devices
        pendingBtAction = BluetoothAction.NONE
        tvDevicesNotice.visibility = View.GONE
        btnBluetoothAction.visibility = View.GONE
        spinnerServerDevice.isEnabled = true
        btnSaveGateway.isEnabled = true

        val labels = devices.map { device ->
            val name = try { device.name } catch (_: SecurityException) { null }
                ?: getString(R.string.device_unnamed)
            "$name (${device.address})"
        }
        val spinnerAdapter = ArrayAdapter(this, R.layout.item_spinner_dark, labels).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown_dark)
        }
        spinnerServerDevice.adapter = spinnerAdapter

        val targetMac = prevMac ?: restoredSelectedMac ?: BTRouterClientService.getTargetMac(this)
        if (!targetMac.isNullOrBlank()) {
            val idx = devices.indexOfFirst { it.address.equals(targetMac, ignoreCase = true) }
            if (idx >= 0) {
                spinnerServerDevice.setSelection(idx)
            }
        }
        restoredSelectedMac = null
    }

    private fun showDeviceNotice(message: String, buttonText: String, action: BluetoothAction) {
        bondedDevicesList = emptyList()
        pendingBtAction = action
        tvDevicesNotice.text = message
        tvDevicesNotice.visibility = View.VISIBLE
        btnBluetoothAction.text = buttonText
        btnBluetoothAction.visibility = View.VISIBLE
        spinnerServerDevice.adapter = null
        spinnerServerDevice.isEnabled = false
        btnSaveGateway.isEnabled = false
    }

    private fun saveGatewayConfig() {
        val pos = spinnerServerDevice.selectedItemPosition
        if (pos !in bondedDevicesList.indices) {
            Toast.makeText(this, getString(R.string.toast_select_server_device), Toast.LENGTH_SHORT).show()
            return
        }
        val selectedDevice = bondedDevicesList[pos]
        val secretInput = etPairingSecret.text.toString().trim()
        if (!BtProtocol.isValidSecret(secretInput)) {
            Toast.makeText(this, getString(R.string.toast_secret_too_short), Toast.LENGTH_LONG).show()
            return
        }

        val deviceName = try { selectedDevice.name } catch (_: SecurityException) { null } ?: "Server"
        BTRouterClientService.saveServerConfig(this, selectedDevice.address, deviceName, secretInput)
        if (clientService != null) {
            clientService?.onConfigUpdated()
        } else {
            startAndBindService()
        }

        tvGatewayStatus.text = getString(R.string.settings_saved_connecting, deviceName)
        Toast.makeText(this, getString(R.string.toast_gateway_saved), Toast.LENGTH_SHORT).show()
    }

    // --- Реализация BTRouterClientService.ServiceCallback ---

    override fun onStatusUpdated(
        batteryPct: Int,
        isCharging: Boolean,
        operator: String,
        signalLevel: Int,
        dataStatus: String
    ) {
        tvGatewayStatus.text = clientService?.lastStatusText ?: getString(R.string.status_connected_encrypted)
    }

    override fun onConnectionStateChanged(connected: Boolean, statusMessage: String) {
        tvGatewayStatus.text = statusMessage
    }

    override fun onIncomingCall(callerDisplay: String) = Unit

    override fun onCallEnded() = Unit

    override fun onSmsQueueChanged(activeSms: BTRouterClientService.ActiveSms?, totalCount: Int) = Unit

    override fun onSyncDone(count: Int, hasMore: Boolean) {
        tvSyncStatus.text = if (hasMore) {
            getString(R.string.sync_done_more, count)
        } else {
            getString(R.string.sync_done_complete, count)
        }
        tvSyncStatus.visibility = View.VISIBLE
    }

    override fun onSyncEmpty() {
        tvSyncStatus.text = getString(R.string.sync_empty)
        tvSyncStatus.visibility = View.VISIBLE
    }

    override fun onStatusMessage(message: String) {
        tvGatewayStatus.text = message
    }

    override fun onCommandResult(
        cmdId: String,
        cmd: String,
        ok: Boolean,
        message: String,
        restoredText: String?
    ) {
        val text = if (ok) message else getString(R.string.status_error_prefix, message)
        if (cmd == "SYNC_5MIN" || cmd == "SYNC_DISCONNECT") {
            tvSyncStatus.text = text
            tvSyncStatus.visibility = View.VISIBLE
        } else {
            tvGatewayStatus.text = text
        }
    }
}
