package com.exantt.btrouterclient

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity(), ChatRepository.Listener, BTRouterClientService.ServiceCallback {

    companion object {
        const val EXTRA_CHAT_KEY = "extra_chat_key"
        private const val STATE_UNREAD_DIVIDER_ID = "unread_divider_msg_id"
    }

    private sealed class DisplayItem {
        data class DateDivider(val dayKey: Int, val label: String) : DisplayItem()
        object UnreadDivider : DisplayItem()
        data class Message(val msg: ChatLogic.ChatMessage) : DisplayItem()
    }

    private lateinit var chatRepo: ChatRepository
    private lateinit var chatKey: String
    private var currentChat: ChatLogic.ChatThread? = null
    private val messages = ArrayList<ChatLogic.ChatMessage>()
    private var unreadDividerBeforeId: Long? = null

    private var clientService: BTRouterClientService? = null
    private var isBound = false

    private lateinit var chatRoot: LinearLayout
    private lateinit var btnChatBack: Button
    private lateinit var tvChatHeaderTitle: TextView
    private lateinit var tvChatHeaderSubtitle: TextView
    private lateinit var tvChatArchivedTag: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var btnMarkAllReadScroll: LinearLayout
    private lateinit var tvMarkAllReadText: TextView
    private lateinit var tvRepliesUnsupported: TextView
    private lateinit var layoutComposer: LinearLayout
    private lateinit var etMessageInput: EditText
    private lateinit var btnSendMessage: ImageButton

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var messageAdapter: ChatMessageAdapter
    private var hasPerformedInitialScroll = false
    private var isDispatchingSms = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as BTRouterClientService.LocalBinder
            clientService = localBinder.getService()
            clientService?.addCallback(this@ChatActivity)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            clientService = null
            isBound = false
        }
    }

    override fun onStatusUpdated(
        batteryPct: Int,
        isCharging: Boolean,
        operator: String,
        signalLevel: Int,
        dataStatus: String
    ) {}

    override fun onConnectionStateChanged(connected: Boolean, statusMessage: String) {}
    override fun onIncomingCall(callerDisplay: String) {}
    override fun onCallEnded() {}
    override fun onSyncDone(count: Int, hasMore: Boolean) {}
    override fun onSyncEmpty() {}
    override fun onStatusMessage(message: String) {}
    override fun onCommandResult(
        cmdId: String,
        cmd: String,
        ok: Boolean,
        message: String,
        restoredText: String?
    ) {}

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_chat)

        chatKey = intent.getStringExtra(EXTRA_CHAT_KEY).orEmpty()
        if (chatKey.isBlank()) {
            finish()
            return
        }

        chatRepo = ChatRepository.getInstance(this)
        val initialChat = chatRepo.getChat(chatKey)
        if (initialChat == null) {
            finish()
            return
        }
        currentChat = initialChat

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_UNREAD_DIVIDER_ID)) {
            val savedId = savedInstanceState.getLong(STATE_UNREAD_DIVIDER_ID, -1L)
            unreadDividerBeforeId = if (savedId > 0L) savedId else null
        }

        chatRoot = findViewById(R.id.chatRoot)
        btnChatBack = findViewById(R.id.btnChatBack)
        tvChatHeaderTitle = findViewById(R.id.tvChatHeaderTitle)
        tvChatHeaderSubtitle = findViewById(R.id.tvChatHeaderSubtitle)
        tvChatArchivedTag = findViewById(R.id.tvChatArchivedTag)
        rvMessages = findViewById(R.id.rvMessages)
        btnMarkAllReadScroll = findViewById(R.id.btnMarkAllReadScroll)
        tvMarkAllReadText = findViewById(R.id.tvMarkAllReadText)
        tvRepliesUnsupported = findViewById(R.id.tvRepliesUnsupported)
        layoutComposer = findViewById(R.id.layoutComposer)
        etMessageInput = findViewById(R.id.etMessageInput)
        btnSendMessage = findViewById(R.id.btnSendMessage)

        applySystemInsets()

        btnChatBack.setOnClickListener { finish() }

        layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = false
        }
        messageAdapter = ChatMessageAdapter()
        rvMessages.layoutManager = layoutManager
        rvMessages.adapter = messageAdapter
        rvMessages.itemAnimator = null

        rvMessages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                markVisibleMessagesAsRead()
                updateFloatingScrollButton()
            }
        })

        rvMessages.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (oldBottom > 0 && bottom < oldBottom) {
                val atBottomBefore = !rvMessages.canScrollVertically(1) ||
                    layoutManager.findLastVisibleItemPosition() >= messageAdapter.itemCount - 2
                if (atBottomBefore && messageAdapter.itemCount > 0) {
                    rvMessages.post {
                        if (messageAdapter.itemCount > 0) {
                            rvMessages.scrollToPosition(messageAdapter.itemCount - 1)
                        }
                    }
                }
            }
        }

        btnMarkAllReadScroll.setOnClickListener {
            scrollToBottomAndMarkAllRead()
        }

        updateSendButtonState()
        etMessageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updateSendButtonState()
            }
        })

        btnSendMessage.setOnClickListener {
            sendMessage()
        }

        bindChatStateFromRepository(isInitialLoad = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_UNREAD_DIVIDER_ID, unreadDividerBeforeId ?: -1L)
    }

    override fun onStart() {
        super.onStart()
        chatRepo.addListener(this)
        if (isBound && clientService != null) {
            clientService?.addCallback(this)
        } else if (CompatUtils.hasBluetoothConnectPermission(this)) {
            isBound = bindService(Intent(this, BTRouterClientService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        chatRepo.activeChatKey = chatKey
        if (hasPerformedInitialScroll) {
            bindChatStateFromRepository(isInitialLoad = false)
        }
    }

    override fun onPause() {
        super.onPause()
        if (chatRepo.activeChatKey == chatKey) {
            chatRepo.activeChatKey = null
        }
    }

    override fun onStop() {
        super.onStop()
        chatRepo.removeListener(this)
        clientService?.removeCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            try { unbindService(serviceConnection) } catch (_: Exception) {}
            isBound = false
        }
    }

    override fun onChatsChanged(changedKey: String?) {
        if (changedKey == null || changedKey == chatKey) {
            bindChatStateFromRepository(isInitialLoad = false)
        }
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(chatRoot) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }
    }

    private fun updateSendButtonState() {
        val hasText = !etMessageInput.text.isNullOrBlank() && !isDispatchingSms
        btnSendMessage.isEnabled = hasText
        btnSendMessage.alpha = if (hasText) 1.0f else 0.45f
    }

    private fun renderHeader(chat: ChatLogic.ChatThread) {
        val isEng = AppLocale.isEnglish(this)
        if (chat.displayName.isNotBlank()) {
            tvChatHeaderTitle.text = chat.displayName
            tvChatHeaderSubtitle.text = chat.address
            tvChatHeaderSubtitle.visibility = View.VISIBLE
        } else {
            tvChatHeaderTitle.text = ServerMessageLocalizer.localizeSenderNumber(chat.address, isEng)
            tvChatHeaderSubtitle.visibility = View.GONE
        }
        tvChatArchivedTag.visibility = if (chat.isArchived) View.VISIBLE else View.GONE

        if (ChatLogic.isRepliableAddress(chat.address)) {
            layoutComposer.visibility = View.VISIBLE
            tvRepliesUnsupported.visibility = View.GONE
        } else {
            layoutComposer.visibility = View.GONE
            tvRepliesUnsupported.visibility = View.VISIBLE
        }
    }

    private fun bindChatStateFromRepository(isInitialLoad: Boolean, forceScrollToBottom: Boolean = false) {
        val latestChat = chatRepo.getChat(chatKey)
        if (latestChat == null) {
            finish()
            return
        }
        currentChat = latestChat
        renderHeader(latestChat)

        val latestMessages = chatRepo.getMessages(chatKey)

        if (!isInitialLoad && !forceScrollToBottom && hasSameMessagesAndStatuses(messages, latestMessages)) {
            updateFloatingScrollButton()
            return
        }

        val wasAtBottom = !isInitialLoad && !rvMessages.canScrollVertically(1)
        val oldLastId = messages.lastOrNull()?.id ?: 0L

        messages.clear()
        messages.addAll(latestMessages)

        if (isInitialLoad && unreadDividerBeforeId == null) {
            unreadDividerBeforeId = messages.firstOrNull { it.isIncoming && !it.isRead }?.id
        } else if (!isInitialLoad && !wasAtBottom && !forceScrollToBottom && unreadDividerBeforeId == null) {
            val newUnread = messages.firstOrNull { it.isIncoming && !it.isRead && it.id > oldLastId }
            if (newUnread != null) {
                unreadDividerBeforeId = newUnread.id
            }
        }

        val displayItems = buildDisplayItems(messages, unreadDividerBeforeId)
        messageAdapter.submitList(displayItems) {
            if (forceScrollToBottom || wasAtBottom) {
                if (displayItems.isNotEmpty()) {
                    rvMessages.scrollToPosition(displayItems.lastIndex)
                }
                rvMessages.post {
                    markVisibleMessagesAsRead()
                    updateFloatingScrollButton()
                }
            } else if (isInitialLoad) {
                hasPerformedInitialScroll = true
                val dividerIdx = displayItems.indexOfFirst { it is DisplayItem.UnreadDivider }
                if (dividerIdx >= 0) {
                    layoutManager.scrollToPositionWithOffset(dividerIdx, dpToPx(16))
                } else if (displayItems.isNotEmpty()) {
                    rvMessages.scrollToPosition(displayItems.lastIndex)
                }
                rvMessages.post {
                    markVisibleMessagesAsRead()
                    updateFloatingScrollButton()
                }
            } else {
                rvMessages.post {
                    updateFloatingScrollButton()
                }
            }
        }
    }

    private fun hasSameMessagesAndStatuses(
        oldList: List<ChatLogic.ChatMessage>,
        newList: List<ChatLogic.ChatMessage>
    ): Boolean {
        if (oldList.size != newList.size) return false
        for (i in oldList.indices) {
            val a = oldList[i]
            val b = newList[i]
            if (a.id != b.id || a.status != b.status || a.body != b.body) {
                return false
            }
        }
        return true
    }

    private fun buildDisplayItems(
        source: List<ChatLogic.ChatMessage>,
        dividerBeforeMsgId: Long?
    ): List<DisplayItem> {
        val result = ArrayList<DisplayItem>(source.size + 4)
        var prevDayKey = -1
        for (msg in source) {
            val dayKey = ChatLogic.formatDayKey(msg.timestampMs)
            if (dayKey != prevDayKey) {
                result.add(DisplayItem.DateDivider(dayKey, formatDateLabel(msg.timestampMs)))
                prevDayKey = dayKey
            }
            if (dividerBeforeMsgId != null && msg.id == dividerBeforeMsgId) {
                result.add(DisplayItem.UnreadDivider)
            }
            result.add(DisplayItem.Message(msg))
        }
        return result
    }

    private fun markVisibleMessagesAsRead() {
        val total = messageAdapter.itemCount
        if (rvMessages.childCount == 0 || total == 0 || messages.isEmpty()) return

        val lastVisibleId = if (!rvMessages.canScrollVertically(1)) {
            messages.last().id
        } else {
            val lastAny = layoutManager.findLastVisibleItemPosition()
            if (lastAny == RecyclerView.NO_POSITION) return

            var pos = layoutManager.findLastCompletelyVisibleItemPosition()
            if (pos == RecyclerView.NO_POSITION) {
                pos = lastAny
            } else if (lastAny > pos) {
                val partialView = layoutManager.findViewByPosition(lastAny)
                if (partialView != null && (partialView.bottom <= rvMessages.height || partialView.height > rvMessages.height / 2)) {
                    pos = lastAny
                }
            }
            messageAdapter.highestMessageIdUpTo(pos)
        }

        if (lastVisibleId <= 0L) return

        val toMark = ChatLogic.computeUnreadIdsToMarkRead(messages, lastVisibleId)
        if (toMark.isEmpty()) return

        for (i in messages.indices) {
            val m = messages[i]
            if (m.isIncoming && !m.isRead && m.id <= lastVisibleId) {
                messages[i] = m.copy(isRead = true)
            }
        }
        chatRepo.markMessagesReadUpTo(chatKey, lastVisibleId)
        updateFloatingScrollButton()
    }

    private fun scrollToBottomAndMarkAllRead() {
        for (i in messages.indices) {
            if (messages[i].isIncoming && !messages[i].isRead) {
                messages[i] = messages[i].copy(isRead = true)
            }
        }
        unreadDividerBeforeId = null
        chatRepo.markAllRead(chatKey)

        val rebuilt = buildDisplayItems(messages, null)
        messageAdapter.submitList(rebuilt) {
            if (rebuilt.isNotEmpty()) {
                rvMessages.smoothScrollToPosition(rebuilt.lastIndex)
            }
            updateFloatingScrollButton()
        }
    }

    private fun updateFloatingScrollButton() {
        val remainingUnread = messages.count { it.isIncoming && !it.isRead }
        val atBottom = !rvMessages.canScrollVertically(1)

        when {
            remainingUnread > 0 -> {
                btnMarkAllReadScroll.visibility = View.VISIBLE
                btnMarkAllReadScroll.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2196F3"))
                tvMarkAllReadText.text = getString(R.string.chat_mark_all_read_btn, remainingUnread)
                tvMarkAllReadText.visibility = View.VISIBLE
            }
            !atBottom && messageAdapter.itemCount > 0 -> {
                btnMarkAllReadScroll.visibility = View.VISIBLE
                btnMarkAllReadScroll.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2C2C2E"))
                tvMarkAllReadText.visibility = View.GONE
            }
            else -> {
                btnMarkAllReadScroll.visibility = View.GONE
            }
        }
    }

    private fun sendMessage() {
        if (isDispatchingSms) return
        val chat = currentChat ?: return
        val text = etMessageInput.text?.toString().orEmpty()
        if (text.isBlank()) return

        val svc = clientService
        if (svc == null) {
            Toast.makeText(this, getString(R.string.status_service_not_bound), Toast.LENGTH_SHORT).show()
            return
        }

        isDispatchingSms = true
        updateSendButtonState()
        try {
            val result = svc.sendSmsDetailed(
                rawNumber = chat.address,
                text = text,
                displayName = chat.displayName.ifBlank { null }
            )
            if (!result.ok) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                return
            }

            etMessageInput.text.clear()
            unreadDividerBeforeId = null
            chatRepo.markAllRead(chatKey)
            bindChatStateFromRepository(isInitialLoad = false, forceScrollToBottom = true)
        } finally {
            isDispatchingSms = false
            updateSendButtonState()
        }
    }

    private fun formatDateLabel(timestampMs: Long): String {
        val nowMs = System.currentTimeMillis()
        val msgKey = ChatLogic.formatDayKey(timestampMs)
        val todayKey = ChatLogic.formatDayKey(nowMs)
        if (msgKey == todayKey) return getString(R.string.chat_day_today)

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayKey = ChatLogic.formatDayKey(yesterdayCal.timeInMillis)
        if (msgKey == yesterdayKey) return getString(R.string.chat_day_yesterday)

        val msgCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val locale = Locale.forLanguageTag(AppLocale.getLanguage(this))
        val pattern = if (msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            "d MMMM"
        } else {
            "d MMMM yyyy"
        }
        return SimpleDateFormat(pattern, locale).format(Date(timestampMs))
    }

    private fun copyTextToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BTRouter SMS", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, getString(R.string.chat_message_copied), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMessageActionsDialog(msg: ChatLogic.ChatMessage) {
        val actions = arrayOf(
            getString(R.string.msg_action_copy),
            getString(R.string.msg_action_delete)
        )
        AlertDialog.Builder(this)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> copyTextToClipboard(msg.body)
                    1 -> confirmDeleteMessage(msg)
                }
            }
            .show()
    }

    private fun confirmDeleteMessage(msg: ChatLogic.ChatMessage) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_message_title)
            .setMessage(R.string.dialog_delete_message_confirm)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.btn_delete) { _, _ ->
                if (unreadDividerBeforeId == msg.id) {
                    unreadDividerBeforeId = null
                }
                if (chatRepo.deleteMessage(chatKey, msg.id)) {
                    Toast.makeText(this, getString(R.string.toast_message_deleted), Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private inner class ChatMessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_DATE = 0
        private val TYPE_UNREAD = 1
        private val TYPE_INCOMING = 2
        private val TYPE_OUTGOING = 3

        private var items: List<DisplayItem> = emptyList()

        fun submitList(newItems: List<DisplayItem>, onCommitted: (() -> Unit)? = null) {
            val oldItems = items
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int = oldItems.size
                override fun getNewListSize(): Int = newItems.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val a = oldItems[oldItemPosition]
                    val b = newItems[newItemPosition]
                    return when {
                        a is DisplayItem.DateDivider && b is DisplayItem.DateDivider -> a.dayKey == b.dayKey
                        a is DisplayItem.UnreadDivider && b is DisplayItem.UnreadDivider -> true
                        a is DisplayItem.Message && b is DisplayItem.Message -> a.msg.id == b.msg.id
                        else -> false
                    }
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldItems[oldItemPosition] == newItems[newItemPosition]
                }
            })
            items = newItems
            diff.dispatchUpdatesTo(this)
            onCommitted?.invoke()
        }

        fun highestMessageIdUpTo(position: Int): Long {
            if (items.isEmpty()) return 0L
            val capped = minOf(position, items.lastIndex)
            var maxId = 0L
            for (i in 0..capped) {
                val item = items[i]
                if (item is DisplayItem.Message && item.msg.id > maxId) {
                    maxId = item.msg.id
                }
            }
            return maxId
        }

        override fun getItemViewType(position: Int): Int {
            return when (val item = items[position]) {
                is DisplayItem.DateDivider -> TYPE_DATE
                is DisplayItem.UnreadDivider -> TYPE_UNREAD
                is DisplayItem.Message -> if (item.msg.isIncoming) TYPE_INCOMING else TYPE_OUTGOING
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return when (viewType) {
                TYPE_DATE -> DateViewHolder(inflater.inflate(R.layout.item_chat_date_divider, parent, false))
                TYPE_UNREAD -> UnreadViewHolder(inflater.inflate(R.layout.item_chat_unread_divider, parent, false))
                TYPE_INCOMING -> IncomingViewHolder(inflater.inflate(R.layout.item_chat_message_incoming, parent, false))
                else -> OutgoingViewHolder(inflater.inflate(R.layout.item_chat_message_outgoing, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is DisplayItem.DateDivider -> (holder as DateViewHolder).bind(item)
                is DisplayItem.UnreadDivider -> Unit
                is DisplayItem.Message -> {
                    if (holder is IncomingViewHolder) {
                        holder.bind(item.msg)
                    } else if (holder is OutgoingViewHolder) {
                        holder.bind(item.msg)
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size

        private inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val tvDate: TextView = view.findViewById(R.id.tvDateDivider)
            fun bind(item: DisplayItem.DateDivider) {
                tvDate.text = item.label
            }
        }

        private inner class UnreadViewHolder(view: View) : RecyclerView.ViewHolder(view)

        private inner class IncomingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val bubbleContainer: View = view.findViewById(R.id.bubbleContainer)
            private val tvBody: TextView = view.findViewById(R.id.tvMessageBody)
            private val layoutOtp: LinearLayout = view.findViewById(R.id.layoutBubbleOtp)
            private val tvOtpCode: TextView = view.findViewById(R.id.tvBubbleOtpCode)
            private val btnCopyOtp: Button = view.findViewById(R.id.btnBubbleCopyOtp)
            private val tvTime: TextView = view.findViewById(R.id.tvMessageTime)

            fun bind(msg: ChatLogic.ChatMessage) {
                val locale = Locale.forLanguageTag(AppLocale.getLanguage(itemView.context))
                tvBody.text = msg.body
                tvTime.text = SimpleDateFormat("HH:mm", locale).format(Date(msg.timestampMs))

                val otp = msg.otpCode
                if (!otp.isNullOrEmpty()) {
                    tvOtpCode.text = otp
                    btnCopyOtp.text = getString(R.string.btn_copy_otp_code, otp)
                    btnCopyOtp.setOnClickListener {
                        OtpCopyReceiver.copyOtpToClipboard(itemView.context, otp, showToast = true)
                    }
                    layoutOtp.visibility = View.VISIBLE
                } else {
                    layoutOtp.visibility = View.GONE
                }

                bubbleContainer.setOnLongClickListener {
                    showMessageActionsDialog(msg)
                    true
                }
            }
        }

        private inner class OutgoingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val bubbleContainer: View = view.findViewById(R.id.bubbleContainer)
            private val tvBody: TextView = view.findViewById(R.id.tvMessageBody)
            private val tvStatus: TextView = view.findViewById(R.id.tvMessageStatus)
            private val tvTime: TextView = view.findViewById(R.id.tvMessageTime)

            fun bind(msg: ChatLogic.ChatMessage) {
                val locale = Locale.forLanguageTag(AppLocale.getLanguage(itemView.context))
                tvBody.text = msg.body
                tvTime.text = SimpleDateFormat("HH:mm", locale).format(Date(msg.timestampMs))

                when (msg.status) {
                    ChatLogic.STATUS_SENDING -> {
                        tvStatus.text = getString(R.string.chat_msg_status_sending)
                        tvStatus.setTextColor(Color.parseColor("#C2D9FF"))
                        tvStatus.visibility = View.VISIBLE
                    }
                    ChatLogic.STATUS_QUEUED -> {
                        tvStatus.text = getString(R.string.chat_msg_status_queued)
                        tvStatus.setTextColor(Color.parseColor("#FFD54F"))
                        tvStatus.visibility = View.VISIBLE
                    }
                    ChatLogic.STATUS_FAILED -> {
                        tvStatus.text = getString(R.string.chat_msg_status_failed)
                        tvStatus.setTextColor(Color.parseColor("#FF8A80"))
                        tvStatus.visibility = View.VISIBLE
                    }
                    ChatLogic.STATUS_UNKNOWN -> {
                        tvStatus.text = getString(R.string.chat_msg_status_unknown)
                        tvStatus.setTextColor(Color.parseColor("#FFD54F"))
                        tvStatus.visibility = View.VISIBLE
                    }
                    else -> {
                        tvStatus.visibility = View.GONE
                    }
                }

                bubbleContainer.setOnLongClickListener {
                    showMessageActionsDialog(msg)
                    true
                }
            }
        }
    }
}
