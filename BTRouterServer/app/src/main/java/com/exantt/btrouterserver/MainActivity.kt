package com.exantt.btrouterserver

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity(), BTRouterServerService.ServiceCallback {

    companion object {
        private const val STATE_SELECTED_TAB = "selected_tab"
        private const val TAB_MAIN = 0
        private const val TAB_SETTINGS = 1
    }

    private var serverService: BTRouterServerService? = null
    private var isBound = false
    private var isSecretVisible = false
    private var selectedTabIndex = TAB_MAIN
    private var appliedLanguage: String = ""

    private lateinit var rootContainer: LinearLayout
    private lateinit var rootScrollView: ScrollView
    private lateinit var tabLayout: TabLayout
    private lateinit var tabMainContent: LinearLayout
    private lateinit var tabSettingsContent: LinearLayout
    private lateinit var tvClientConnBadge: TextView
    private lateinit var tvServerState: TextView
    private lateinit var tvAllowedClient: TextView
    private lateinit var btnSelectClient: Button
    private lateinit var tvPairingSecret: TextView
    private lateinit var btnToggleSecret: Button
    private lateinit var btnCopySecret: Button
    private lateinit var btnRegenerateSecret: Button
    private lateinit var btnOpenAppSettings: Button
    private lateinit var btnOpenBtSettings: Button
    private lateinit var btnRequestPermissions: Button
    private lateinit var btnLangRu: Button
    private lateinit var btnLangEn: Button
    private lateinit var layoutDebugTests: LinearLayout

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BTRouterServerService.LocalBinder
            val svc = localBinder.getService()
            serverService = svc
            svc.addCallback(this@MainActivity)
            isBound = true
            syncUiWithService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverService?.removeCallback(this@MainActivity)
            serverService = null
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

        rootContainer = findViewById(R.id.rootContainer)
        rootScrollView = findViewById(R.id.rootScrollView)
        tabLayout = findViewById(R.id.tabLayout)
        tabMainContent = findViewById(R.id.tabMainContent)
        tabSettingsContent = findViewById(R.id.tabSettingsContent)
        tvClientConnBadge = findViewById(R.id.tvClientConnBadge)
        tvServerState = findViewById(R.id.tvServerState)
        tvAllowedClient = findViewById(R.id.tvAllowedClient)
        btnSelectClient = findViewById(R.id.btnSelectClient)
        tvPairingSecret = findViewById(R.id.tvPairingSecret)
        btnToggleSecret = findViewById(R.id.btnToggleSecret)
        btnCopySecret = findViewById(R.id.btnCopySecret)
        btnRegenerateSecret = findViewById(R.id.btnRegenerateSecret)
        btnOpenAppSettings = findViewById(R.id.btnOpenAppSettings)
        btnOpenBtSettings = findViewById(R.id.btnOpenBtSettings)
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions)
        btnLangRu = findViewById(R.id.btnLangRu)
        btnLangEn = findViewById(R.id.btnLangEn)
        layoutDebugTests = findViewById(R.id.layoutDebugTests)

        applySystemInsets()
        setupVersionLabel()
        setupTabs(savedInstanceState)

        btnSelectClient.setOnClickListener {
            showSelectClientDialog()
        }

        btnToggleSecret.setOnClickListener {
            isSecretVisible = !isSecretVisible
            updateSecurityViews()
        }

        btnCopySecret.setOnClickListener {
            val secret = BTRouterServerService.getOrCreatePairingSecret(this)
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("BTRouter Key", secret)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            cm.setPrimaryClip(clip)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(this, getString(R.string.toast_secret_copied), Toast.LENGTH_SHORT).show()
            }
        }

        btnRegenerateSecret.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_regen_title)
                .setMessage(R.string.dialog_regen_message)
                .setPositiveButton(R.string.btn_renew) { _, _ ->
                    BTRouterServerService.regeneratePairingSecret(this)
                    serverService?.onConfigUpdated()
                    isSecretVisible = true
                    updateSecurityViews()
                    Toast.makeText(this, getString(R.string.toast_new_secret_generated), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }

        btnOpenAppSettings.setOnClickListener {
            openSystemSettings(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
            )
        }

        btnOpenBtSettings.setOnClickListener {
            openSystemSettings(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }

        btnRequestPermissions.setOnClickListener {
            requestNeededPermissions(forcePromptAll = true)
        }

        updateLanguageButtonsUi()
        btnLangRu.setOnClickListener { selectLanguage(AppLocale.LANG_RU) }
        btnLangEn.setOnClickListener { selectLanguage(AppLocale.LANG_EN) }

        if (BuildConfig.DEBUG) {
            layoutDebugTests.visibility = View.VISIBLE
            findViewById<Button>(R.id.btnTestCall).setOnClickListener {
                serverService?.sendTestCall()
            }
            findViewById<Button>(R.id.btnTestMissedCall).setOnClickListener {
                serverService?.sendTestMissedCall()
            }
            findViewById<Button>(R.id.btnTestStatus).setOnClickListener {
                serverService?.sendStatusNow()
            }
        } else {
            layoutDebugTests.visibility = View.GONE
        }

        updateSecurityViews()
        if (savedInstanceState == null) {
            requestNeededPermissions(forcePromptAll = false)
        }
        if (!CompatUtils.hasBluetoothConnectPermission(this)) {
            tvServerState.text = getString(R.string.status_grant_bt_start)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SELECTED_TAB, selectedTabIndex)
    }

    private fun setupTabs(savedInstanceState: Bundle?) {
        val restored = savedInstanceState?.getInt(STATE_SELECTED_TAB, TAB_MAIN) ?: TAB_MAIN
        selectedTabIndex = if (restored in TAB_MAIN..TAB_SETTINGS) restored else TAB_MAIN

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTabIndex = tab.position
                showTab(tab.position)
                rootScrollView.scrollTo(0, 0)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) {
                rootScrollView.smoothScrollTo(0, 0)
            }
        })

        if (selectedTabIndex != tabLayout.selectedTabPosition) {
            tabLayout.getTabAt(selectedTabIndex)?.select()
        }
        showTab(selectedTabIndex)
    }

    private fun showTab(index: Int) {
        val showSettings = index == TAB_SETTINGS
        tabMainContent.visibility = if (showSettings) View.GONE else View.VISIBLE
        tabSettingsContent.visibility = if (showSettings) View.VISIBLE else View.GONE
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
        serverService?.onLanguageChanged()
        recreate()
    }

    private fun openSystemSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, getString(R.string.toast_open_settings_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySystemInsets() {
        rootScrollView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, windowInsets ->
            val topBarsAndCutout = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeAndBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            rootContainer.setPadding(topBarsAndCutout.left, topBarsAndCutout.top, topBarsAndCutout.right, 0)
            rootScrollView.setPadding(0, 0, 0, imeAndBars.bottom)
            windowInsets
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
        findViewById<TextView>(R.id.tvAppVersion).text = "v$versionName"
    }

    private fun requestNeededPermissions(forcePromptAll: Boolean) {
        val candidates = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            candidates.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            candidates.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = if (forcePromptAll) {
            candidates
        } else {
            candidates.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
        }
    }

    private fun startAndBindService() {
        if (!CompatUtils.hasBluetoothConnectPermission(this)) return
        val serviceIntent = Intent(this, BTRouterServerService::class.java)
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
        val svc = serverService
        if (isBound && svc != null) {
            svc.addCallback(this)
            syncUiWithService()
        } else if (CompatUtils.hasBluetoothConnectPermission(this)) {
            startAndBindService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (CompatUtils.hasBluetoothConnectPermission(this)) {
            startAndBindService()
        } else {
            tvServerState.text = getString(R.string.status_bt_permission_required)
        }
    }

    override fun onStop() {
        super.onStop()
        serverService?.removeCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {
            }
            isBound = false
        }
    }

    override fun onServiceStateChanged(stateText: String, isConnected: Boolean, connectedClientName: String) {
        tvServerState.text = stateText
        if (isConnected) {
            tvClientConnBadge.text = getString(R.string.badge_online)
            tvClientConnBadge.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvClientConnBadge.text = getString(R.string.badge_waiting)
            tvClientConnBadge.setTextColor(Color.parseColor("#FF9800"))
        }
    }

    private fun syncUiWithService() {
        val service = serverService ?: return
        onServiceStateChanged(service.currentStateText, service.isConnected, service.connectedClientLabel)
        updateSecurityViews()
    }

    private fun updateSecurityViews() {
        val mac = BTRouterServerService.getAllowedClientMac(this)
        val clientName = BTRouterServerService.getAllowedClientName(this)

        if (mac.isNullOrBlank()) {
            tvAllowedClient.text = getString(R.string.client_not_selected)
            tvAllowedClient.setTextColor(Color.parseColor("#FF8A80"))
        } else {
            val label = if (!clientName.isNullOrBlank()) "$clientName ($mac)" else mac
            tvAllowedClient.text = getString(R.string.allowed_client_format, label)
            tvAllowedClient.setTextColor(Color.parseColor("#E5E5EA"))
        }

        if (isSecretVisible) {
            tvPairingSecret.text = BTRouterServerService.getOrCreatePairingSecret(this)
            btnToggleSecret.text = getString(R.string.btn_hide_secret)
        } else {
            tvPairingSecret.text = "••••-••••-••••-••••"
            btnToggleSecret.text = getString(R.string.btn_show_secret)
        }
    }

    private fun showSelectClientDialog() {
        if (!CompatUtils.hasBluetoothConnectPermission(this)) {
            Toast.makeText(this, getString(R.string.toast_grant_bt_first), Toast.LENGTH_SHORT).show()
            requestNeededPermissions(forcePromptAll = false)
            return
        }

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = btManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, getString(R.string.toast_enable_bt_server), Toast.LENGTH_SHORT).show()
            return
        }

        val devices: List<BluetoothDevice> = try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        if (devices.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_bonded_title)
                .setMessage(R.string.dialog_no_bonded_message)
                .setPositiveButton(R.string.btn_ok, null)
                .show()
            return
        }

        val items = devices.map { device ->
            val name = try { device.name } catch (_: SecurityException) { null } ?: getString(R.string.device_unnamed)
            "$name\n${device.address}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_select_client_title)
            .setItems(items) { _, index ->
                val chosen = devices[index]
                val chosenName = try { chosen.name } catch (_: SecurityException) { null } ?: "Client"
                BTRouterServerService.setAllowedClient(this, chosen.address, chosenName)
                if (serverService != null) {
                    serverService?.onConfigUpdated()
                } else if (CompatUtils.hasBluetoothConnectPermission(this)) {
                    startAndBindService()
                }
                updateSecurityViews()
                Toast.makeText(this, getString(R.string.toast_client_saved), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
