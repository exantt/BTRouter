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
    private lateinit var layoutDebugTests: LinearLayout

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BTRouterServerService.LocalBinder
            serverService = localBinder.getService()
            serverService?.callback = this@MainActivity
            isBound = true
            syncUiWithService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serverService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            Toast.makeText(this, "Ключ сопряжения скопирован", Toast.LENGTH_SHORT).show()
        }

        btnRegenerateSecret.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Обновить ключ сопряжения?")
                .setMessage("Текущее соединение с Client будет разорвано. Новый ключ нужно будет ввести на Client.")
                .setPositiveButton("Обновить") { _, _ ->
                    BTRouterServerService.regeneratePairingSecret(this)
                    serverService?.onConfigUpdated()
                    isSecretVisible = true
                    updateSecurityViews()
                    Toast.makeText(this, "Новый ключ сгенерирован", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Отмена", null)
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
            requestNeededPermissions()
        }

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
        requestNeededPermissions()
        if (CompatUtils.hasBluetoothConnectPermission(this)) {
            startAndBindService()
        } else {
            tvServerState.text = "Выдайте разрешение Bluetooth для запуска сервера..."
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

    private fun openSystemSettings(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось открыть системные настройки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applySystemInsets() {
        rootScrollView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(rootContainer) { _, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            rootContainer.setPadding(insets.left, insets.top, insets.right, 0)
            rootScrollView.setPadding(0, 0, 0, insets.bottom)
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

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
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
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
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
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStart() {
        super.onStart()
        if (isBound) {
            serverService?.callback = this
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
            tvServerState.text = "Требуется разрешение Bluetooth (устройства поблизости)"
        }
    }

    override fun onStop() {
        super.onStop()
        if (serverService?.callback === this) {
            serverService?.callback = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onServiceStateChanged(stateText: String, isConnected: Boolean, connectedClientName: String) {
        tvServerState.text = stateText
        if (isConnected) {
            tvClientConnBadge.text = "● В СЕТИ"
            tvClientConnBadge.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            tvClientConnBadge.text = "● ОЖИДАНИЕ"
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
            tvAllowedClient.text = "Client не выбран (подключения отклоняются)"
            tvAllowedClient.setTextColor(Color.parseColor("#FF8A80"))
        } else {
            val label = if (!clientName.isNullOrBlank()) "$clientName ($mac)" else mac
            tvAllowedClient.text = "Разрешён: $label"
            tvAllowedClient.setTextColor(Color.parseColor("#E5E5EA"))
        }

        if (isSecretVisible) {
            tvPairingSecret.text = BTRouterServerService.getOrCreatePairingSecret(this)
            btnToggleSecret.text = "СКРЫТЬ"
        } else {
            tvPairingSecret.text = "••••-••••-••••-••••"
            btnToggleSecret.text = "ПОКАЗАТЬ"
        }
    }

    private fun showSelectClientDialog() {
        if (!CompatUtils.hasBluetoothConnectPermission(this)) {
            Toast.makeText(this, "Сначала выдайте разрешение Bluetooth", Toast.LENGTH_SHORT).show()
            requestNeededPermissions()
            return
        }

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = btManager.adapter
        if (adapter == null || !adapter.isEnabled) {
            Toast.makeText(this, "Включите Bluetooth на Server", Toast.LENGTH_SHORT).show()
            return
        }

        val devices: List<BluetoothDevice> = try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }

        if (devices.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Нет спаренных устройств")
                .setMessage("Сначала спарьте Client и Server через системные настройки Bluetooth.")
                .setPositiveButton("ОК", null)
                .show()
            return
        }

        val items = devices.map { device ->
            val name = try { device.name } catch (_: SecurityException) { null } ?: "Без имени"
            "$name\n${device.address}"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Выберите устройство Client")
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
                Toast.makeText(this, "Устройство Client сохранено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}
