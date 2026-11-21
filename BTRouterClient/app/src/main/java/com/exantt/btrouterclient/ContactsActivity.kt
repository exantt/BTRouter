package com.exantt.btrouterclient

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.Executors

class ContactsActivity : AppCompatActivity(), BTRouterClientService.ServiceCallback {

    companion object {
        const val EXTRA_SELECTED_NUMBER = "extra_selected_number"
        const val EXTRA_SELECTED_NAME = "extra_selected_name"

        private const val REQ_READ_CONTACTS = 101
        private const val SERVER_LOAD_TIMEOUT_MS = 10_000L
        private const val SERVER_CACHE_FRESH_MS = 5_000L
        private const val STATE_SOURCE = "selected_contacts_source"
        private val HIGHLIGHT_BG_COLOR = Color.parseColor("#66FFC107")
    }

    private enum class CardPosition {
        SINGLE,
        TOP,
        MIDDLE,
        BOTTOM
    }

    private sealed class ListItem {
        data class Header(val title: String) : ListItem()
        data class LetterHeader(val letter: String) : ListItem()
        data class Contact(val entry: ContactsHelper.ContactEntry, val cardPosition: CardPosition) : ListItem()
    }

    private var clientService: BTRouterClientService? = null
    private var isBound = false
    private var appliedLanguage: String = ""
    private var currentSource: String = BTRouterClientService.SOURCE_CLIENT

    private var clientContacts: List<ContactsHelper.ContactEntry> = emptyList()
    private var serverContacts: List<ContactsHelper.ContactEntry> = emptyList()
    private var hasLoadedClient = false
    private var isLoadingClient = false
    private var isLoadingServer = false
    private var isSilentServerRefresh = false
    private var hasPromptedContactsPermission = false
    private var pendingServerLoadOnBind = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val contactsExecutor = Executors.newSingleThreadExecutor()

    private lateinit var contactsRoot: LinearLayout
    private lateinit var btnContactsBack: Button
    private lateinit var btnReloadContacts: Button
    private lateinit var btnSourceClient: Button
    private lateinit var btnSourceServer: Button
    private lateinit var etSearchContacts: EditText
    private lateinit var btnClearSearch: Button
    private lateinit var layoutContactsBanner: LinearLayout
    private lateinit var tvContactsBanner: TextView
    private lateinit var btnContactsBannerAction: Button
    private lateinit var lvContacts: ListView
    private lateinit var tvEmptyContacts: TextView

    private lateinit var contactsAdapter: ContactsListAdapter

    private val serverLoadTimeoutRunnable = Runnable {
        if (!isLoadingServer) return@Runnable
        isLoadingServer = false
        val wasSilent = isSilentServerRefresh
        isSilentServerRefresh = false
        if (currentSource == BTRouterClientService.SOURCE_SERVER) {
            val notice = if (wasSilent && serverContacts.isNotEmpty()) {
                getString(R.string.contacts_cached_refresh_failed)
            } else {
                getString(R.string.contacts_server_timeout)
            }
            showBanner(
                text = notice,
                color = Color.parseColor("#FFB74D"),
                actionText = getString(R.string.btn_retry)
            ) {
                requestServerContactsFromService(silent = false)
            }
            rebuildDisplayedList()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BTRouterClientService.LocalBinder
            val svc = localBinder.getService()
            clientService = svc
            svc.callback = this@ContactsActivity
            isBound = true

            if (svc.hasCachedServerContacts()) {
                serverContacts = svc.getCachedServerContacts()
            }

            if (currentSource == BTRouterClientService.SOURCE_SERVER) {
                val isStale = (System.currentTimeMillis() - svc.getServerContactsLoadedAtMs()) > SERVER_CACHE_FRESH_MS
                when {
                    pendingServerLoadOnBind || !svc.hasCachedServerContacts() -> {
                        pendingServerLoadOnBind = false
                        requestServerContactsFromService(silent = false)
                    }
                    svc.isConnected && isStale && !isLoadingServer -> {
                        rebuildDisplayedList()
                        requestServerContactsFromService(silent = true)
                    }
                    else -> {
                        refreshServerBanner()
                        rebuildDisplayedList()
                    }
                }
            } else {
                rebuildDisplayedList()
            }
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
        appliedLanguage = AppLocale.getLanguage(this)
        setContentView(R.layout.activity_contacts)

        contactsRoot = findViewById(R.id.contactsRoot)
        btnContactsBack = findViewById(R.id.btnContactsBack)
        btnReloadContacts = findViewById(R.id.btnReloadContacts)
        btnSourceClient = findViewById(R.id.btnSourceClient)
        btnSourceServer = findViewById(R.id.btnSourceServer)
        etSearchContacts = findViewById(R.id.etSearchContacts)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        layoutContactsBanner = findViewById(R.id.layoutContactsBanner)
        tvContactsBanner = findViewById(R.id.tvContactsBanner)
        btnContactsBannerAction = findViewById(R.id.btnContactsBannerAction)
        lvContacts = findViewById(R.id.lvContacts)
        tvEmptyContacts = findViewById(R.id.tvEmptyContacts)

        applySystemInsets()

        currentSource = savedInstanceState?.getString(STATE_SOURCE)
            ?: BTRouterClientService.getLastContactsSource(this)

        contactsAdapter = ContactsListAdapter()
        lvContacts.adapter = contactsAdapter
        lvContacts.setOnItemClickListener { _, _, position, _ ->
            val item = contactsAdapter.getItem(position)
            if (item is ListItem.Contact) {
                val entry = item.entry
                val number = ContactsHelper.phoneForInput(entry)
                val result = Intent().apply {
                    putExtra(EXTRA_SELECTED_NUMBER, number)
                    putExtra(EXTRA_SELECTED_NAME, entry.name)
                }
                setResult(RESULT_OK, result)
                finish()
            }
        }

        btnContactsBack.setOnClickListener { finish() }

        btnReloadContacts.setOnClickListener {
            if (currentSource == BTRouterClientService.SOURCE_CLIENT) {
                loadClientContacts(requestPermissionIfMissing = true)
            } else {
                requestServerContactsFromService()
            }
        }

        btnSourceClient.setOnClickListener { switchSource(BTRouterClientService.SOURCE_CLIENT) }
        btnSourceServer.setOnClickListener { switchSource(BTRouterClientService.SOURCE_SERVER) }

        etSearchContacts.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                rebuildDisplayedList()
            }
        })

        btnClearSearch.setOnClickListener {
            etSearchContacts.text.clear()
        }

        updateSourceButtonsUi()
        if (currentSource == BTRouterClientService.SOURCE_CLIENT) {
            loadClientContacts(requestPermissionIfMissing = true)
        } else {
            // Загружаем локальный список в фоне только если уже есть разрешение — пригодится для обогащения имён в «Недавних»
            if (hasReadContactsPermission()) {
                loadClientContacts(requestPermissionIfMissing = false)
            }
            pendingServerLoadOnBind = true
            showBanner(
                text = getString(R.string.contacts_connecting_loading_server),
                color = Color.parseColor("#E5E5EA")
            )
            rebuildDisplayedList()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SOURCE, currentSource)
    }

    override fun onStart() {
        super.onStart()
        if (AppLocale.getLanguage(this) != appliedLanguage) {
            recreate()
            return
        }
        val svc = clientService
        if (isBound && svc != null) {
            svc.callback = this
            if (svc.hasCachedServerContacts()) {
                serverContacts = svc.getCachedServerContacts()
            }
            if (currentSource == BTRouterClientService.SOURCE_SERVER) {
                val isStale = (System.currentTimeMillis() - svc.getServerContactsLoadedAtMs()) > SERVER_CACHE_FRESH_MS
                if (svc.isConnected && isStale && !isLoadingServer) {
                    requestServerContactsFromService(silent = svc.hasCachedServerContacts())
                } else {
                    refreshServerBanner()
                }
            }
            rebuildDisplayedList()
        } else {
            bindService(Intent(this, BTRouterClientService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasReadContactsPermission() && !isLoadingClient) {
            loadClientContacts(requestPermissionIfMissing = false, silent = hasLoadedClient)
        }
    }

    override fun onStop() {
        super.onStop()
        if (clientService?.callback === this) {
            clientService?.callback = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacks(serverLoadTimeoutRunnable)
        contactsExecutor.shutdownNow()
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
        if (requestCode == REQ_READ_CONTACTS) {
            if (hasReadContactsPermission()) {
                loadClientContacts(requestPermissionIfMissing = false)
            } else if (currentSource == BTRouterClientService.SOURCE_CLIENT) {
                showClientPermissionBanner()
                rebuildDisplayedList()
            }
        }
    }

    private fun applySystemInsets() {
        lvContacts.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(contactsRoot) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeAndBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            contactsRoot.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            lvContacts.setPadding(0, 0, 0, imeAndBars.bottom)
            tvEmptyContacts.setPadding(
                tvEmptyContacts.paddingLeft,
                tvEmptyContacts.paddingTop,
                tvEmptyContacts.paddingRight,
                imeAndBars.bottom
            )
            windowInsets
        }
    }

    private fun hasReadContactsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun switchSource(newSource: String) {
        if (currentSource == newSource) return
        currentSource = newSource
        BTRouterClientService.setLastContactsSource(this, newSource)
        updateSourceButtonsUi()

        if (newSource == BTRouterClientService.SOURCE_CLIENT) {
            mainHandler.removeCallbacks(serverLoadTimeoutRunnable)
            isLoadingServer = false
            pendingServerLoadOnBind = false
            if (!hasLoadedClient && !isLoadingClient) {
                loadClientContacts(requestPermissionIfMissing = true)
            } else if (!hasReadContactsPermission()) {
                showClientPermissionBanner()
                rebuildDisplayedList()
            } else {
                hideBanner()
                rebuildDisplayedList()
            }
        } else {
            val svc = clientService
            if (svc == null) {
                pendingServerLoadOnBind = true
                showBanner(
                    text = getString(R.string.contacts_connecting_loading_server),
                    color = Color.parseColor("#E5E5EA")
                )
                rebuildDisplayedList()
            } else if (svc.hasCachedServerContacts()) {
                serverContacts = svc.getCachedServerContacts()
                rebuildDisplayedList()
                val isStale = (System.currentTimeMillis() - svc.getServerContactsLoadedAtMs()) > SERVER_CACHE_FRESH_MS
                if (svc.isConnected && isStale && !isLoadingServer) {
                    requestServerContactsFromService(silent = true)
                } else {
                    refreshServerBanner()
                }
            } else {
                requestServerContactsFromService(silent = false)
            }
        }
    }

    private fun updateSourceButtonsUi() {
        val activeBg = ColorStateList.valueOf(Color.parseColor("#2196F3"))
        val inactiveBg = ColorStateList.valueOf(Color.parseColor("#1C1C1E"))
        val activeText = Color.WHITE
        val inactiveText = Color.parseColor("#8E8E93")

        val isClient = currentSource == BTRouterClientService.SOURCE_CLIENT
        btnSourceClient.backgroundTintList = if (isClient) activeBg else inactiveBg
        btnSourceClient.setTextColor(if (isClient) activeText else inactiveText)
        btnSourceServer.backgroundTintList = if (!isClient) activeBg else inactiveBg
        btnSourceServer.setTextColor(if (!isClient) activeText else inactiveText)
    }

    private fun showClientPermissionBanner() {
        showBanner(
            text = getString(R.string.contacts_grant_permission_notice),
            color = Color.parseColor("#FFB74D"),
            actionText = getString(R.string.btn_grant_contacts_permission)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQ_READ_CONTACTS
            )
        }
    }

    private fun loadClientContacts(requestPermissionIfMissing: Boolean, silent: Boolean = false) {
        if (!hasReadContactsPermission()) {
            if (currentSource == BTRouterClientService.SOURCE_CLIENT) {
                showClientPermissionBanner()
                rebuildDisplayedList()
            }
            if (requestPermissionIfMissing && !hasPromptedContactsPermission) {
                hasPromptedContactsPermission = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    REQ_READ_CONTACTS
                )
            }
            return
        }

        if (isLoadingClient) return
        isLoadingClient = true
        if (!silent && currentSource == BTRouterClientService.SOURCE_CLIENT) {
            showBanner(
                text = getString(R.string.contacts_reading_client),
                color = Color.parseColor("#E5E5EA")
            )
        }

        contactsExecutor.execute {
            val loaded = queryDeviceContacts()
            val prepared = ContactsHelper.sortContactsEnThenRu(
                ContactsHelper.deduplicateContacts(loaded)
            )
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                isLoadingClient = false
                hasLoadedClient = true
                clientContacts = prepared
                if (currentSource == BTRouterClientService.SOURCE_CLIENT) {
                    hideBanner()
                }
                rebuildDisplayedList()
            }
        }
    }

    private fun queryDeviceContacts(): List<ContactsHelper.ContactEntry> {
        val result = ArrayList<ContactsHelper.ContactEntry>(256)
        try {
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
                    val cleanPhone = ContactsHelper.cleanDisplayPhone(rawNumber)
                    val canonical = ContactsHelper.canonicalPhoneKey(cleanPhone)
                    if (ContactsHelper.digitsOnly(canonical).length !in 3..15) continue
                    val rawName = (if (nameIdx >= 0) c.getString(nameIdx) else null) ?: ""
                    val inVisible = if (visIdx >= 0) c.getInt(visIdx) != 0 else true
                    val updatedAt = if (updIdx >= 0) c.getLong(updIdx) else 0L
                    val accountType = if (accIdx >= 0) c.getString(accIdx) else null

                    result.add(
                        ContactsHelper.ContactEntry(
                            name = rawName.trim().ifEmpty { cleanPhone }.take(120),
                            phone = cleanPhone.ifEmpty { canonical },
                            normalizedPhone = canonical,
                            inVisibleGroup = inVisible,
                            isPrimaryAccount = ContactsHelper.isPrimaryContactAccount(accountType),
                            lastUpdatedMs = updatedAt
                        )
                    )
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun requestServerContactsFromService(silent: Boolean = false) {
        val svc = clientService
        if (svc == null) {
            pendingServerLoadOnBind = true
            return
        }
        mainHandler.removeCallbacks(serverLoadTimeoutRunnable)
        isSilentServerRefresh = silent
        val (started, msg) = svc.requestServerContacts()
        if (started) {
            isLoadingServer = true
            mainHandler.postDelayed(serverLoadTimeoutRunnable, SERVER_LOAD_TIMEOUT_MS)
            if (!silent && currentSource == BTRouterClientService.SOURCE_SERVER) {
                showBanner(
                    text = msg,
                    color = Color.parseColor("#E5E5EA")
                )
            }
        } else {
            isLoadingServer = false
            isSilentServerRefresh = false
            if (svc.hasCachedServerContacts()) {
                serverContacts = svc.getCachedServerContacts()
            }
            if (currentSource == BTRouterClientService.SOURCE_SERVER) {
                if (silent && serverContacts.isNotEmpty()) {
                    refreshServerBanner()
                } else {
                    showBanner(
                        text = msg,
                        color = Color.parseColor("#FFB74D"),
                        actionText = getString(R.string.btn_retry)
                    ) {
                        requestServerContactsFromService(silent = false)
                    }
                }
            }
        }
        rebuildDisplayedList()
    }

    private fun refreshServerBanner() {
        if (isLoadingServer) return
        val svc = clientService ?: return
        if (!svc.isConnected && svc.hasCachedServerContacts()) {
            showBanner(
                text = getString(R.string.contacts_offline_cached),
                color = Color.parseColor("#FFB74D"),
                actionText = getString(R.string.btn_retry)
            ) {
                requestServerContactsFromService()
            }
        } else {
            hideBanner()
        }
    }

    private fun showBanner(
        text: String,
        color: Int,
        actionText: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        tvContactsBanner.text = text
        tvContactsBanner.setTextColor(color)
        if (actionText != null && onAction != null) {
            btnContactsBannerAction.text = actionText
            btnContactsBannerAction.visibility = View.VISIBLE
            btnContactsBannerAction.setOnClickListener { onAction() }
        } else {
            btnContactsBannerAction.visibility = View.GONE
            btnContactsBannerAction.setOnClickListener(null)
        }
        layoutContactsBanner.visibility = View.VISIBLE
    }

    private fun hideBanner() {
        layoutContactsBanner.visibility = View.GONE
        btnContactsBannerAction.visibility = View.GONE
        btnContactsBannerAction.setOnClickListener(null)
    }

    private fun rebuildDisplayedList() {
        val query = etSearchContacts.text.toString()
        val isClientSource = currentSource == BTRouterClientService.SOURCE_CLIENT
        val activeBook = if (isClientSource) clientContacts else serverContacts
        val secondaryBook = if (isClientSource) serverContacts else clientContacts

        val namesByExactDigits = HashMap<String, String>(activeBook.size + secondaryBook.size + 8)
        val namesByLast10 = HashMap<String, String>(activeBook.size + secondaryBook.size + 8)
        val ambiguousLast10 = HashSet<String>()

        fun registerBookNames(book: List<ContactsHelper.ContactEntry>, overrideExisting: Boolean) {
            val localSeenLast10 = HashSet<String>()
            for (c in book) {
                if (c.name.isBlank() || c.name == c.phone || ContactsHelper.canonicalPhoneKey(c.name) == c.normalizedPhone) {
                    continue
                }
                val digits = ContactsHelper.digitsOnly(c.normalizedPhone)
                if (digits.isEmpty()) continue
                if (overrideExisting || !namesByExactDigits.containsKey(digits)) {
                    namesByExactDigits[digits] = c.name
                }
                if (digits.length >= 10) {
                    val last10 = digits.takeLast(10)
                    if (!localSeenLast10.add(last10)) {
                        ambiguousLast10.add(last10)
                    } else if (!ambiguousLast10.contains(last10) && (overrideExisting || !namesByLast10.containsKey(last10))) {
                        namesByLast10[last10] = c.name
                    }
                }
            }
        }

        registerBookNames(secondaryBook, overrideExisting = false)
        registerBookNames(activeBook, overrideExisting = true)
        for (amb in ambiguousLast10) {
            namesByLast10.remove(amb)
        }

        val enrichedRecent = BTRouterClientService.getRecentSmsContacts(this).map { recent ->
            val latestName = ContactsHelper.findLatestNameForPhone(
                recent.normalizedPhone,
                namesByExactDigits,
                namesByLast10
            )
            if (!latestName.isNullOrBlank()) {
                recent.copy(name = latestName)
            } else {
                recent
            }
        }

        val filteredRecent = ContactsHelper.filterContacts(enrichedRecent, query).take(5)
        val filteredBook = ContactsHelper.filterContacts(activeBook, query)

        val items = ArrayList<ListItem>(filteredRecent.size + filteredBook.size + 32)
        if (filteredRecent.isNotEmpty()) {
            items.add(ListItem.Header(getString(R.string.contacts_section_recent)))
            appendContactGroup(items, filteredRecent)
        }

        val sourceLabel = if (isClientSource) {
            getString(R.string.source_client)
        } else {
            getString(R.string.source_server)
        }
        if (filteredBook.isNotEmpty()) {
            val headerTitle = if (query.isBlank()) {
                getString(R.string.contacts_section_book, sourceLabel, filteredBook.size)
            } else {
                getString(R.string.contacts_section_found, sourceLabel, filteredBook.size)
            }
            items.add(ListItem.Header(headerTitle))

            var currentLetter: String? = null
            val currentGroup = ArrayList<ContactsHelper.ContactEntry>()
            for (c in filteredBook) {
                val letter = ContactsHelper.sectionLetter(c.name)
                if (letter != currentLetter) {
                    if (currentGroup.isNotEmpty()) {
                        appendContactGroup(items, currentGroup)
                        currentGroup.clear()
                    }
                    currentLetter = letter
                    items.add(ListItem.LetterHeader(letter))
                }
                currentGroup.add(c)
            }
            if (currentGroup.isNotEmpty()) {
                appendContactGroup(items, currentGroup)
            }
        }

        contactsAdapter.updateData(items, query)

        val loadingNow = (isClientSource && isLoadingClient) || (!isClientSource && isLoadingServer)
        if (items.isEmpty() && !loadingNow) {
            lvContacts.visibility = View.GONE
            tvEmptyContacts.visibility = View.VISIBLE
            tvEmptyContacts.text = if (query.isNotBlank()) {
                getString(R.string.contacts_empty_search)
            } else {
                getString(R.string.contacts_empty_list)
            }
        } else {
            tvEmptyContacts.visibility = View.GONE
            lvContacts.visibility = View.VISIBLE
        }
    }

    // --- Реализация BTRouterClientService.ServiceCallback ---

    override fun onStatusUpdated(
        batteryPct: Int,
        isCharging: Boolean,
        operator: String,
        signalLevel: Int,
        dataStatus: String
    ) = Unit

    override fun onConnectionStateChanged(connected: Boolean, statusMessage: String) {
        if (currentSource == BTRouterClientService.SOURCE_SERVER && connected &&
            !isLoadingServer && clientService?.hasCachedServerContacts() == false
        ) {
            requestServerContactsFromService()
        }
    }

    override fun onIncomingCall(callerDisplay: String) = Unit

    override fun onCallEnded() = Unit

    override fun onSmsQueueChanged(activeSms: BTRouterClientService.ActiveSms?, totalCount: Int) = Unit

    override fun onSyncDone(count: Int, hasMore: Boolean) = Unit

    override fun onSyncEmpty() = Unit

    override fun onStatusMessage(message: String) = Unit

    override fun onCommandResult(
        cmdId: String,
        cmd: String,
        ok: Boolean,
        message: String,
        restoredText: String?
    ) = Unit

    override fun onServerContactsLoaded(
        ok: Boolean,
        contacts: List<ContactsHelper.ContactEntry>,
        errorMessage: String?
    ) {
        mainHandler.removeCallbacks(serverLoadTimeoutRunnable)
        isLoadingServer = false
        val wasSilent = isSilentServerRefresh
        isSilentServerRefresh = false
        serverContacts = contacts

        if (currentSource == BTRouterClientService.SOURCE_SERVER) {
            if (ok) {
                hideBanner()
            } else {
                val notice = if (wasSilent && contacts.isNotEmpty()) {
                    getString(R.string.contacts_cached_refresh_failed)
                } else {
                    errorMessage ?: getString(R.string.contacts_load_server_failed)
                }
                showBanner(
                    text = notice,
                    color = Color.parseColor("#FFB74D"),
                    actionText = getString(R.string.btn_retry)
                ) {
                    requestServerContactsFromService(silent = false)
                }
            }
            rebuildDisplayedList()
        }
    }

    private inner class ContactsListAdapter : BaseAdapter() {

        private var items: List<ListItem> = emptyList()
        private var currentQuery: String = ""

        fun updateData(newItems: List<ListItem>, query: String) {
            items = newItems
            currentQuery = query
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): ListItem = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getViewTypeCount(): Int = 3

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is ListItem.Header -> 0
                is ListItem.LetterHeader -> 1
                is ListItem.Contact -> 2
            }
        }

        override fun areAllItemsEnabled(): Boolean = false

        override fun isEnabled(position: Int): Boolean = items[position] is ListItem.Contact

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = items[position]
            val inflater = LayoutInflater.from(parent.context)

            return when (item) {
                is ListItem.Header -> {
                    val view = convertView ?: inflater.inflate(R.layout.item_contact_header, parent, false)
                    view.findViewById<TextView>(R.id.tvHeaderTitle).text = item.title
                    view
                }
                is ListItem.LetterHeader -> {
                    val view = convertView ?: inflater.inflate(R.layout.item_contact_letter, parent, false)
                    view.findViewById<TextView>(R.id.tvLetterTitle).text = item.letter
                    view
                }
                is ListItem.Contact -> {
                    val view = convertView ?: inflater.inflate(R.layout.item_contact_row, parent, false)
                    val holder = (view.tag as? ContactViewHolder) ?: ContactViewHolder(
                        cardContainer = view.findViewById(R.id.contactCardContainer),
                        tvName = view.findViewById(R.id.tvContactName),
                        tvPhone = view.findViewById(R.id.tvContactPhone)
                    ).also { view.tag = it }

                    val bgRes = when (item.cardPosition) {
                        CardPosition.SINGLE -> R.drawable.bg_contact_card_single
                        CardPosition.TOP -> R.drawable.bg_contact_card_top
                        CardPosition.MIDDLE -> R.drawable.bg_contact_card_middle
                        CardPosition.BOTTOM -> R.drawable.bg_contact_card_bottom
                    }
                    holder.cardContainer.setBackgroundResource(bgRes)

                    val entry = item.entry
                    val phoneRange = ContactsHelper.findPhoneHighlightRange(entry.phone, currentQuery)
                    if (entry.name.isBlank() || entry.name == entry.phone) {
                        holder.tvName.text = highlightText(entry.phone, phoneRange)
                        holder.tvPhone.text = ""
                    } else {
                        val nameRange = ContactsHelper.findNameHighlightRange(entry.name, currentQuery)
                        holder.tvName.text = highlightText(entry.name, nameRange)
                        holder.tvPhone.text = highlightText(entry.phone, phoneRange)
                    }
                    view
                }
            }
        }

        private fun highlightText(text: String, range: IntRange?): CharSequence {
            if (range == null || range.first < 0 || range.last >= text.length || range.first > range.last) {
                return text
            }
            val spannable = SpannableString(text)
            spannable.setSpan(
                BackgroundColorSpan(HIGHLIGHT_BG_COLOR),
                range.first,
                range.last + 1,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return spannable
        }
    }

    private fun appendContactGroup(
        dest: MutableList<ListItem>,
        group: List<ContactsHelper.ContactEntry>
    ) {
        val lastIdx = group.lastIndex
        for (i in group.indices) {
            val pos = when {
                group.size == 1 -> CardPosition.SINGLE
                i == 0 -> CardPosition.TOP
                i == lastIdx -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }
            dest.add(ListItem.Contact(group[i], pos))
        }
    }

    private data class ContactViewHolder(
        val cardContainer: LinearLayout,
        val tvName: TextView,
        val tvPhone: TextView
    )
}
