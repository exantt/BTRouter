package com.exantt.btrouterclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NewSmsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PREFILL_NUMBER = "extra_prefill_number"
        const val EXTRA_PREFILL_NAME = "extra_prefill_name"
        private const val STATE_PICKED_CONTACT_NAME = "picked_contact_name"
        private const val STATE_PICKED_CONTACT_NUMBER = "picked_contact_number"
    }

    private var clientService: BTRouterClientService? = null
    private var isBound = false
    private var pickedContactName: String? = null
    private var pickedContactNumber: String? = null

    private lateinit var newSmsRoot: LinearLayout
    private lateinit var newSmsScrollView: ScrollView
    private lateinit var btnNewSmsBack: Button
    private lateinit var etTargetNumber: EditText
    private lateinit var btnContacts: ImageButton
    private lateinit var etNewSmsText: EditText
    private lateinit var btnSendNewSms: Button
    private lateinit var tvNewSmsStatus: TextView

    private val contactsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val number = result.data?.getStringExtra(ContactsActivity.EXTRA_SELECTED_NUMBER).orEmpty()
            val name = result.data?.getStringExtra(ContactsActivity.EXTRA_SELECTED_NAME)
            if (number.isNotEmpty()) {
                pickedContactName = name
                pickedContactNumber = number
                etTargetNumber.setText(number)
                etTargetNumber.setSelection(number.length)
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BTRouterClientService.LocalBinder
            clientService = localBinder.getService()
            isBound = true
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
        setContentView(R.layout.activity_new_sms)

        newSmsRoot = findViewById(R.id.newSmsRoot)
        newSmsScrollView = findViewById(R.id.newSmsScrollView)
        btnNewSmsBack = findViewById(R.id.btnNewSmsBack)
        etTargetNumber = findViewById(R.id.etTargetNumber)
        btnContacts = findViewById(R.id.btnContacts)
        etNewSmsText = findViewById(R.id.etNewSmsText)
        btnSendNewSms = findViewById(R.id.btnSendNewSms)
        tvNewSmsStatus = findViewById(R.id.tvNewSmsStatus)

        applySystemInsets()

        if (savedInstanceState != null) {
            pickedContactName = savedInstanceState.getString(STATE_PICKED_CONTACT_NAME)
            pickedContactNumber = savedInstanceState.getString(STATE_PICKED_CONTACT_NUMBER)
        } else {
            val prefillNumber = intent.getStringExtra(EXTRA_PREFILL_NUMBER).orEmpty()
            val prefillName = intent.getStringExtra(EXTRA_PREFILL_NAME)
            if (prefillNumber.isNotBlank()) {
                pickedContactName = prefillName?.takeIf { it.isNotBlank() }
                pickedContactNumber = prefillNumber
                etTargetNumber.setText(prefillNumber)
                etTargetNumber.setSelection(prefillNumber.length)
                etNewSmsText.requestFocus()
            }
        }

        btnNewSmsBack.setOnClickListener { finish() }

        btnContacts.setOnClickListener {
            contactsLauncher.launch(Intent(this, ContactsActivity::class.java))
        }

        btnSendNewSms.setOnClickListener {
            val number = etTargetNumber.text.toString()
            val text = etNewSmsText.text.toString()
            val prevNumber = pickedContactNumber
            val contactName = if (prevNumber != null &&
                ContactsHelper.canonicalPhoneKey(number) == ContactsHelper.canonicalPhoneKey(prevNumber)
            ) {
                pickedContactName
            } else {
                null
            }

            val svc = clientService
            if (svc == null) {
                showStatusError(getString(R.string.status_service_not_bound))
                return@setOnClickListener
            }

            val result = svc.sendSmsDetailed(number, text, contactName)
            if (!result.ok) {
                showStatusError(result.message)
                return@setOnClickListener
            }

            val targetChatKey = result.chatKey
                ?: ChatRepository.getInstance(this).resolveChatKeyForAddress(number)
            val chatIntent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CHAT_KEY, targetChatKey)
            }
            startActivity(chatIntent)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (pickedContactName != null) {
            outState.putString(STATE_PICKED_CONTACT_NAME, pickedContactName)
        }
        if (pickedContactNumber != null) {
            outState.putString(STATE_PICKED_CONTACT_NUMBER, pickedContactNumber)
        }
    }

    override fun onStart() {
        super.onStart()
        if (!isBound && CompatUtils.hasBluetoothConnectPermission(this)) {
            bindService(Intent(this, BTRouterClientService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun applySystemInsets() {
        newSmsScrollView.clipToPadding = false
        ViewCompat.setOnApplyWindowInsetsListener(newSmsRoot) { _, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeAndBars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            newSmsRoot.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            newSmsScrollView.setPadding(0, 0, 0, imeAndBars.bottom)
            windowInsets
        }
    }

    private fun showStatusError(msg: String) {
        tvNewSmsStatus.text = msg
        tvNewSmsStatus.visibility = View.VISIBLE
    }
}
