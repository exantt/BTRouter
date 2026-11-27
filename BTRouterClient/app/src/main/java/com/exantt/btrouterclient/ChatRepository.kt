package com.exantt.btrouterclient

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Локальное хранилище чатов, сообщений и пропущенных вызовов на базе SQLite.
 */
class ChatRepository private constructor(context: Context) {

    interface Listener {
        fun onChatsChanged(chatKey: String?) {}
        fun onMissedCallsChanged() {}
    }

    data class IncomingSmsResult(
        val chatKey: String,
        val wasArchived: Boolean,
        val inserted: Boolean
    )

    companion object {
        private const val DB_NAME = "btrouter_chats.db"
        private const val DB_VERSION = 1
        private const val MAX_MISSED_CALLS_STORED = 200

        private const val TABLE_CHATS = "chats"
        private const val TABLE_MESSAGES = "messages"
        private const val TABLE_MISSED_CALLS = "missed_calls"

        @Volatile
        private var instance: ChatRepository? = null

        fun getInstance(context: Context): ChatRepository {
            return instance ?: synchronized(this) {
                instance ?: ChatRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    @Volatile
    var activeChatKey: String? = null

    private val dbLock = Any()
    private val notifyLock = Any()
    private var notifyScheduled = false
    private var notifyAllPending = false
    private var notifySingleKey: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<Listener>()
    private val dbHelper = DbHelper(context.applicationContext)

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun notifyChatsChanged(chatKey: String?) {
        if (listeners.isEmpty()) return
        synchronized(notifyLock) {
            if (chatKey == null) {
                notifyAllPending = true
                notifySingleKey = null
            } else if (!notifyAllPending) {
                if (notifySingleKey == null && !notifyScheduled) {
                    notifySingleKey = chatKey
                } else if (notifySingleKey != chatKey) {
                    notifyAllPending = true
                    notifySingleKey = null
                }
            }
            if (notifyScheduled) return
            notifyScheduled = true
        }
        mainHandler.post {
            val dispatchKey: String?
            synchronized(notifyLock) {
                dispatchKey = if (notifyAllPending) null else notifySingleKey
                notifyScheduled = false
                notifyAllPending = false
                notifySingleKey = null
            }
            for (listener in listeners) {
                listener.onChatsChanged(dispatchKey)
            }
        }
    }

    private fun notifyMissedCallsChanged() {
        if (listeners.isEmpty()) return
        mainHandler.post {
            for (listener in listeners) {
                listener.onMissedCallsChanged()
            }
        }
    }

    fun hasMessageEventId(eventId: String): Boolean {
        if (eventId.isBlank()) return false
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                "SELECT 1 FROM $TABLE_MESSAGES WHERE event_id = ? LIMIT 1",
                arrayOf(eventId)
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        }
    }

    fun hasMissedCallEventId(eventId: String): Boolean {
        if (eventId.isBlank()) return false
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                "SELECT 1 FROM $TABLE_MISSED_CALLS WHERE event_id = ? LIMIT 1",
                arrayOf(eventId)
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        }
    }

    fun isChatArchivedByAddress(rawAddress: String): Boolean {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            val resolvedKey = resolveChatKeyLocked(db, rawAddress)
            db.rawQuery(
                "SELECT is_archived FROM $TABLE_CHATS WHERE chat_key = ? LIMIT 1",
                arrayOf(resolvedKey)
            ).use { cursor ->
                return cursor.moveToFirst() && cursor.getInt(0) == 1
            }
        }
    }

    fun resolveChatKeyForAddress(rawAddress: String): String {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            return resolveChatKeyLocked(db, rawAddress)
        }
    }

    private fun resolveChatKeyLocked(db: SQLiteDatabase, rawAddress: String): String {
        val existingKeys = ArrayList<String>()
        db.rawQuery("SELECT chat_key FROM $TABLE_CHATS", null).use { cursor ->
            while (cursor.moveToNext()) {
                existingKeys.add(cursor.getString(0))
            }
        }
        return ChatLogic.findMatchingPhoneChatKey(rawAddress, existingKeys)
    }

    fun recordIncomingSms(
        eventId: String?,
        rawNumber: String,
        senderName: String,
        body: String,
        timestampMs: Long,
        otpCode: String?,
        markRead: Boolean = false,
        isSync: Boolean = false
    ): IncomingSmsResult {
        var resolvedKey: String
        var wasArchived = false
        var inserted = false

        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                if (!eventId.isNullOrBlank()) {
                    db.rawQuery(
                        "SELECT chat_key FROM $TABLE_MESSAGES WHERE event_id = ? LIMIT 1",
                        arrayOf(eventId)
                    ).use { c ->
                        if (c.moveToFirst()) {
                            resolvedKey = c.getString(0)
                            db.rawQuery(
                                "SELECT is_archived FROM $TABLE_CHATS WHERE chat_key = ? LIMIT 1",
                                arrayOf(resolvedKey)
                            ).use { ac ->
                                wasArchived = ac.moveToFirst() && ac.getInt(0) == 1
                            }
                            db.setTransactionSuccessful()
                            return IncomingSmsResult(resolvedKey, wasArchived, inserted = false)
                        }
                    }
                }

                resolvedKey = resolveChatKeyLocked(db, rawNumber)
                val cleanAddress = rawNumber.trim().ifEmpty { resolvedKey }
                val cleanName = senderName.trim()

                var existingAddress = ""
                var existingName = ""
                var existingLastTs = 0L
                var chatExists = false

                db.rawQuery(
                    "SELECT address, display_name, is_archived, last_message_ts FROM $TABLE_CHATS WHERE chat_key = ? LIMIT 1",
                    arrayOf(resolvedKey)
                ).use { c ->
                    if (c.moveToFirst()) {
                        chatExists = true
                        existingAddress = c.getString(0).orEmpty()
                        existingName = c.getString(1).orEmpty()
                        wasArchived = c.getInt(2) == 1
                        existingLastTs = c.getLong(3)
                    }
                }

                if (isSync && chatExists) {
                    db.rawQuery(
                        """
                        SELECT 1 FROM $TABLE_MESSAGES
                        WHERE chat_key = ? AND is_incoming = 1 AND event_id LIKE 'sms_live_%'
                          AND body = ? AND ABS(timestamp_ms - ?) <= 120000
                        LIMIT 1
                        """.trimIndent(),
                        arrayOf(resolvedKey, body, timestampMs.toString())
                    ).use { dupCursor ->
                        if (dupCursor.moveToFirst()) {
                            db.setTransactionSuccessful()
                            return IncomingSmsResult(resolvedKey, wasArchived, inserted = false)
                        }
                    }
                }

                val effectiveTs = ChatLogic.resolveMessageTimestamp(
                    rawTimestampMs = timestampMs,
                    receivedAtMs = System.currentTimeMillis(),
                    existingLastTs = existingLastTs,
                    isSync = isSync
                )

                val msgValues = ContentValues().apply {
                    put("chat_key", resolvedKey)
                    if (!eventId.isNullOrBlank()) put("event_id", eventId) else putNull("event_id")
                    put("body", body)
                    put("timestamp_ms", effectiveTs)
                    put("is_incoming", 1)
                    put("is_read", if (markRead) 1 else 0)
                    put("status", ChatLogic.STATUS_SENT)
                    if (!otpCode.isNullOrBlank()) put("otp_code", otpCode) else putNull("otp_code")
                }
                db.insertOrThrow(TABLE_MESSAGES, null, msgValues)
                inserted = true

                val bestName = if (cleanName.isNotEmpty()) cleanName else existingName
                val bestAddress = pickBetterAddress(existingAddress, cleanAddress)
                val shouldUpdatePreview = !chatExists || effectiveTs >= existingLastTs

                if (!chatExists) {
                    val chatValues = ContentValues().apply {
                        put("chat_key", resolvedKey)
                        put("address", bestAddress)
                        put("display_name", bestName)
                        put("is_pinned", 0)
                        put("pinned_at", 0L)
                        put("is_archived", 0)
                        put("last_message_body", body)
                        put("last_message_ts", effectiveTs)
                        put("last_message_incoming", 1)
                    }
                    db.insertOrThrow(TABLE_CHATS, null, chatValues)
                } else {
                    val chatValues = ContentValues().apply {
                        put("address", bestAddress)
                        if (bestName.isNotEmpty()) {
                            put("display_name", bestName)
                        }
                        if (shouldUpdatePreview) {
                            put("last_message_body", body)
                            put("last_message_ts", effectiveTs)
                            put("last_message_incoming", 1)
                        }
                    }
                    db.update(TABLE_CHATS, chatValues, "chat_key = ?", arrayOf(resolvedKey))
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        if (inserted) {
            notifyChatsChanged(resolvedKey)
        }
        return IncomingSmsResult(resolvedKey, wasArchived, inserted)
    }

    fun recordOutgoingSms(
        cmdId: String,
        rawNumber: String,
        displayName: String?,
        body: String,
        timestampMs: Long,
        initialStatus: Int
    ): String {
        val resolvedKey: String
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                resolvedKey = resolveChatKeyLocked(db, rawNumber)
                val cleanAddress = rawNumber.trim().ifEmpty { resolvedKey }
                val cleanName = displayName?.trim().orEmpty()

                var existingAddress = ""
                var existingName = ""
                var existingLastTs = 0L
                var chatExists = false

                db.rawQuery(
                    "SELECT address, display_name, last_message_ts FROM $TABLE_CHATS WHERE chat_key = ? LIMIT 1",
                    arrayOf(resolvedKey)
                ).use { c ->
                    if (c.moveToFirst()) {
                        chatExists = true
                        existingAddress = c.getString(0).orEmpty()
                        existingName = c.getString(1).orEmpty()
                        existingLastTs = c.getLong(2)
                    }
                }

                val effectiveTs = ChatLogic.resolveMessageTimestamp(
                    rawTimestampMs = timestampMs,
                    receivedAtMs = timestampMs,
                    existingLastTs = existingLastTs,
                    isSync = false
                )

                val msgValues = ContentValues().apply {
                    put("chat_key", resolvedKey)
                    if (cmdId.isNotBlank()) put("event_id", cmdId) else putNull("event_id")
                    put("body", body)
                    put("timestamp_ms", effectiveTs)
                    put("is_incoming", 0)
                    put("is_read", 1)
                    put("status", initialStatus)
                    putNull("otp_code")
                }
                db.insertOrThrow(TABLE_MESSAGES, null, msgValues)

                val bestName = if (cleanName.isNotEmpty() && !cleanName.equals(cleanAddress, ignoreCase = true)) {
                    cleanName
                } else {
                    existingName
                }
                val bestAddress = pickBetterAddress(existingAddress, cleanAddress)
                val shouldUpdatePreview = !chatExists || effectiveTs >= existingLastTs

                if (!chatExists) {
                    val chatValues = ContentValues().apply {
                        put("chat_key", resolvedKey)
                        put("address", bestAddress)
                        put("display_name", bestName)
                        put("is_pinned", 0)
                        put("pinned_at", 0L)
                        put("is_archived", 0)
                        put("last_message_body", body)
                        put("last_message_ts", effectiveTs)
                        put("last_message_incoming", 0)
                    }
                    db.insertOrThrow(TABLE_CHATS, null, chatValues)
                } else {
                    val chatValues = ContentValues().apply {
                        put("address", bestAddress)
                        put("is_archived", 0)
                        if (bestName.isNotEmpty()) {
                            put("display_name", bestName)
                        }
                        if (shouldUpdatePreview) {
                            put("last_message_body", body)
                            put("last_message_ts", effectiveTs)
                            put("last_message_incoming", 0)
                        }
                    }
                    db.update(TABLE_CHATS, chatValues, "chat_key = ?", arrayOf(resolvedKey))
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

        notifyChatsChanged(resolvedKey)
        return resolvedKey
    }

    fun updateOutgoingMessageStatus(cmdId: String, newStatus: Int) {
        if (cmdId.isBlank()) return
        var affectedChatKey: String? = null
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.rawQuery(
                "SELECT chat_key, status FROM $TABLE_MESSAGES WHERE event_id = ? AND is_incoming = 0 LIMIT 1",
                arrayOf(cmdId)
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val chatKey = cursor.getString(0)
                    val currentStatus = cursor.getInt(1)
                    if (ChatLogic.canTransitionOutgoingStatus(currentStatus, newStatus)) {
                        affectedChatKey = chatKey
                    }
                }
            }
            if (affectedChatKey != null) {
                val values = ContentValues().apply {
                    put("status", newStatus)
                }
                db.update(
                    TABLE_MESSAGES,
                    values,
                    "event_id = ? AND is_incoming = 0",
                    arrayOf(cmdId)
                )
            }
        }
        if (affectedChatKey != null) {
            notifyChatsChanged(affectedChatKey)
        }
    }

    fun markStaleSendingAsUnknown(): Int {
        val updated: Int
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("status", ChatLogic.STATUS_UNKNOWN)
            }
            updated = db.update(
                TABLE_MESSAGES,
                values,
                "is_incoming = 0 AND status = ?",
                arrayOf(ChatLogic.STATUS_SENDING.toString())
            )
        }
        if (updated > 0) {
            notifyChatsChanged(null)
        }
        return updated
    }

    fun getActiveChats(): List<ChatLogic.ChatThread> {
        val list = queryChatsLocked(archived = false)
        return ChatLogic.sortActiveChats(list)
    }

    fun getArchivedChats(): List<ChatLogic.ChatThread> {
        val list = queryChatsLocked(archived = true)
        return ChatLogic.sortArchivedChats(list)
    }

    private fun queryChatsLocked(archived: Boolean): List<ChatLogic.ChatThread> {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            val sql = """
                SELECT c.chat_key, c.address, c.display_name, c.is_pinned, c.pinned_at, c.is_archived,
                       c.last_message_body, c.last_message_ts, c.last_message_incoming,
                       (SELECT COUNT(*) FROM $TABLE_MESSAGES m
                        WHERE m.chat_key = c.chat_key AND m.is_incoming = 1 AND m.is_read = 0) AS unread_count
                FROM $TABLE_CHATS c
                WHERE c.is_archived = ?
            """.trimIndent()
            val result = ArrayList<ChatLogic.ChatThread>()
            db.rawQuery(sql, arrayOf(if (archived) "1" else "0")).use { c ->
                while (c.moveToNext()) {
                    result.add(
                        ChatLogic.ChatThread(
                            chatKey = c.getString(0),
                            address = c.getString(1),
                            displayName = c.getString(2),
                            isPinned = c.getInt(3) == 1,
                            pinnedAt = c.getLong(4),
                            isArchived = c.getInt(5) == 1,
                            lastMessageBody = c.getString(6),
                            lastMessageTs = c.getLong(7),
                            lastMessageIncoming = c.getInt(8) == 1,
                            unreadCount = c.getInt(9)
                        )
                    )
                }
            }
            return result
        }
    }

    fun getArchivedSummary(): Pair<Int, Int> {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            val chatCount: Int
            db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_CHATS WHERE is_archived = 1",
                null
            ).use { c ->
                chatCount = if (c.moveToFirst()) c.getInt(0) else 0
            }
            if (chatCount == 0) return Pair(0, 0)

            val unreadCount: Int
            db.rawQuery(
                """
                SELECT COUNT(*) FROM $TABLE_MESSAGES m
                INNER JOIN $TABLE_CHATS c ON c.chat_key = m.chat_key
                WHERE c.is_archived = 1 AND m.is_incoming = 1 AND m.is_read = 0
                """.trimIndent(),
                null
            ).use { c ->
                unreadCount = if (c.moveToFirst()) c.getInt(0) else 0
            }
            return Pair(chatCount, unreadCount)
        }
    }

    fun getChat(chatKey: String): ChatLogic.ChatThread? {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            val sql = """
                SELECT c.chat_key, c.address, c.display_name, c.is_pinned, c.pinned_at, c.is_archived,
                       c.last_message_body, c.last_message_ts, c.last_message_incoming,
                       (SELECT COUNT(*) FROM $TABLE_MESSAGES m
                        WHERE m.chat_key = c.chat_key AND m.is_incoming = 1 AND m.is_read = 0) AS unread_count
                FROM $TABLE_CHATS c
                WHERE c.chat_key = ? LIMIT 1
            """.trimIndent()
            db.rawQuery(sql, arrayOf(chatKey)).use { c ->
                if (c.moveToFirst()) {
                    return ChatLogic.ChatThread(
                        chatKey = c.getString(0),
                        address = c.getString(1),
                        displayName = c.getString(2),
                        isPinned = c.getInt(3) == 1,
                        pinnedAt = c.getLong(4),
                        isArchived = c.getInt(5) == 1,
                        lastMessageBody = c.getString(6),
                        lastMessageTs = c.getLong(7),
                        lastMessageIncoming = c.getInt(8) == 1,
                        unreadCount = c.getInt(9)
                    )
                }
            }
            return null
        }
    }

    fun getMessages(chatKey: String): List<ChatLogic.ChatMessage> {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            val result = ArrayList<ChatLogic.ChatMessage>()
            db.rawQuery(
                """
                SELECT id, chat_key, event_id, body, timestamp_ms, is_incoming, is_read, status, otp_code
                FROM $TABLE_MESSAGES
                WHERE chat_key = ?
                ORDER BY timestamp_ms ASC, id ASC
                """.trimIndent(),
                arrayOf(chatKey)
            ).use { c ->
                while (c.moveToNext()) {
                    result.add(
                        ChatLogic.ChatMessage(
                            id = c.getLong(0),
                            chatKey = c.getString(1),
                            eventId = c.getString(2),
                            body = c.getString(3),
                            timestampMs = c.getLong(4),
                            isIncoming = c.getInt(5) == 1,
                            isRead = c.getInt(6) == 1,
                            status = c.getInt(7),
                            otpCode = c.getString(8)
                        )
                    )
                }
            }
            return result
        }
    }

    fun markMessagesReadUpTo(chatKey: String, maxMessageId: Long): Int {
        if (maxMessageId <= 0L) return 0
        val updated: Int
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply { put("is_read", 1) }
            updated = db.update(
                TABLE_MESSAGES,
                values,
                "chat_key = ? AND is_incoming = 1 AND is_read = 0 AND id <= ?",
                arrayOf(chatKey, maxMessageId.toString())
            )
        }
        if (updated > 0) {
            notifyChatsChanged(chatKey)
        }
        return updated
    }

    fun markAllRead(chatKey: String): Int {
        val updated: Int
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply { put("is_read", 1) }
            updated = db.update(
                TABLE_MESSAGES,
                values,
                "chat_key = ? AND is_incoming = 1 AND is_read = 0",
                arrayOf(chatKey)
            )
        }
        if (updated > 0) {
            notifyChatsChanged(chatKey)
        }
        return updated
    }

    fun setPinned(chatKey: String, pinned: Boolean): Boolean {
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            if (pinned) {
                var alreadyPinned = false
                db.rawQuery(
                    "SELECT is_pinned FROM $TABLE_CHATS WHERE chat_key = ? LIMIT 1",
                    arrayOf(chatKey)
                ).use { c ->
                    alreadyPinned = c.moveToFirst() && c.getInt(0) == 1
                }
                if (!alreadyPinned) {
                    val currentPinnedCount: Int
                    db.rawQuery(
                        "SELECT COUNT(*) FROM $TABLE_CHATS WHERE is_archived = 0 AND is_pinned = 1",
                        null
                    ).use { c ->
                        currentPinnedCount = if (c.moveToFirst()) c.getInt(0) else 0
                    }
                    if (!ChatLogic.canPinMore(currentPinnedCount)) {
                        return false
                    }
                }
            }

            val values = ContentValues().apply {
                put("is_pinned", if (pinned) 1 else 0)
                put("pinned_at", if (pinned) System.currentTimeMillis() else 0L)
            }
            db.update(TABLE_CHATS, values, "chat_key = ?", arrayOf(chatKey))
        }
        notifyChatsChanged(chatKey)
        return true
    }

    fun setArchived(chatKey: String, archived: Boolean) {
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("is_archived", if (archived) 1 else 0)
                if (archived) {
                    put("is_pinned", 0)
                    put("pinned_at", 0L)
                }
            }
            db.update(TABLE_CHATS, values, "chat_key = ?", arrayOf(chatKey))
        }
        notifyChatsChanged(chatKey)
    }

    fun deleteMessage(chatKey: String, messageId: Long): Boolean {
        if (messageId <= 0L) return false
        var deleted = false
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                val rows = db.delete(
                    TABLE_MESSAGES,
                    "id = ? AND chat_key = ?",
                    arrayOf(messageId.toString(), chatKey)
                )
                if (rows <= 0) {
                    db.setTransactionSuccessful()
                    return false
                }
                deleted = true

                var remainingBody: String? = null
                var remainingTs = 0L
                var remainingIncoming = 1
                db.rawQuery(
                    """
                    SELECT body, timestamp_ms, is_incoming
                    FROM $TABLE_MESSAGES
                    WHERE chat_key = ?
                    ORDER BY timestamp_ms DESC, id DESC
                    LIMIT 1
                    """.trimIndent(),
                    arrayOf(chatKey)
                ).use { c ->
                    if (c.moveToFirst()) {
                        remainingBody = c.getString(0)
                        remainingTs = c.getLong(1)
                        remainingIncoming = c.getInt(2)
                    }
                }

                if (remainingBody != null) {
                    val values = ContentValues().apply {
                        put("last_message_body", remainingBody)
                        put("last_message_ts", remainingTs)
                        put("last_message_incoming", remainingIncoming)
                    }
                    db.update(TABLE_CHATS, values, "chat_key = ?", arrayOf(chatKey))
                } else {
                    db.delete(TABLE_CHATS, "chat_key = ?", arrayOf(chatKey))
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        if (deleted) {
            notifyChatsChanged(chatKey)
        }
        return deleted
    }

    fun deleteChat(chatKey: String) {
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete(TABLE_MESSAGES, "chat_key = ?", arrayOf(chatKey))
                db.delete(TABLE_CHATS, "chat_key = ?", arrayOf(chatKey))
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        notifyChatsChanged(chatKey)
    }

    fun recordMissedCall(
        eventId: String?,
        number: String,
        displayName: String,
        timestampMs: Long
    ): Boolean {
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                if (!eventId.isNullOrBlank()) {
                    db.rawQuery(
                        "SELECT 1 FROM $TABLE_MISSED_CALLS WHERE event_id = ? LIMIT 1",
                        arrayOf(eventId)
                    ).use { c ->
                        if (c.moveToFirst()) {
                            db.setTransactionSuccessful()
                            return false
                        }
                    }
                }

                val values = ContentValues().apply {
                    if (!eventId.isNullOrBlank()) put("event_id", eventId) else putNull("event_id")
                    put("number", number)
                    put("display_name", displayName.trim())
                    put("timestamp_ms", timestampMs)
                }
                db.insertOrThrow(TABLE_MISSED_CALLS, null, values)

                db.execSQL(
                    """
                    DELETE FROM $TABLE_MISSED_CALLS
                    WHERE id NOT IN (
                        SELECT id FROM $TABLE_MISSED_CALLS
                        ORDER BY timestamp_ms DESC, id DESC
                        LIMIT $MAX_MISSED_CALLS_STORED
                    )
                    """.trimIndent()
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        notifyMissedCallsChanged()
        return true
    }

    fun getMissedCalls(): List<ChatLogic.MissedCallRecord> {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            val list = ArrayList<ChatLogic.MissedCallRecord>()
            db.rawQuery(
                """
                SELECT id, event_id, number, display_name, timestamp_ms
                FROM $TABLE_MISSED_CALLS
                ORDER BY timestamp_ms DESC, id DESC
                """.trimIndent(),
                null
            ).use { c ->
                while (c.moveToNext()) {
                    list.add(
                        ChatLogic.MissedCallRecord(
                            id = c.getLong(0),
                            eventId = c.getString(1),
                            number = c.getString(2),
                            displayName = c.getString(3),
                            timestampMs = c.getLong(4)
                        )
                    )
                }
            }
            return list
        }
    }

    fun clearMissedCalls() {
        synchronized(dbLock) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete(TABLE_MISSED_CALLS, null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
        notifyMissedCallsChanged()
    }

    fun getUnreadMissedCallsCount(sinceMs: Long): Int {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_MISSED_CALLS WHERE timestamp_ms > ?",
                arrayOf(sinceMs.toString())
            ).use { c ->
                return if (c.moveToFirst()) c.getInt(0) else 0
            }
        }
    }

    fun getMaxMissedCallId(): Long {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            db.rawQuery("SELECT COALESCE(MAX(id), 0) FROM $TABLE_MISSED_CALLS", null).use { c ->
                return if (c.moveToFirst()) c.getLong(0) else 0L
            }
        }
    }

    fun getUnreadMissedCallsCountAfterId(lastViewedId: Long): Int {
        synchronized(dbLock) {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                "SELECT COUNT(*) FROM $TABLE_MISSED_CALLS WHERE id > ?",
                arrayOf(lastViewedId.toString())
            ).use { c ->
                return if (c.moveToFirst()) c.getInt(0) else 0
            }
        }
    }

    private fun pickBetterAddress(existing: String, incoming: String): String {
        if (existing.isBlank()) return incoming
        if (incoming.isBlank()) return existing
        if (incoming.startsWith("+") && !existing.startsWith("+")) return incoming
        return existing
    }

    private class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
        init {
            setWriteAheadLoggingEnabled(true)
        }
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_CHATS (
                    chat_key TEXT PRIMARY KEY,
                    address TEXT NOT NULL,
                    display_name TEXT NOT NULL DEFAULT '',
                    is_pinned INTEGER NOT NULL DEFAULT 0,
                    pinned_at INTEGER NOT NULL DEFAULT 0,
                    is_archived INTEGER NOT NULL DEFAULT 0,
                    last_message_body TEXT NOT NULL DEFAULT '',
                    last_message_ts INTEGER NOT NULL DEFAULT 0,
                    last_message_incoming INTEGER NOT NULL DEFAULT 1
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE $TABLE_MESSAGES (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_key TEXT NOT NULL,
                    event_id TEXT,
                    body TEXT NOT NULL,
                    timestamp_ms INTEGER NOT NULL,
                    is_incoming INTEGER NOT NULL,
                    is_read INTEGER NOT NULL,
                    status INTEGER NOT NULL DEFAULT 1,
                    otp_code TEXT
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_messages_chat_ts ON $TABLE_MESSAGES(chat_key, timestamp_ms, id)")
            db.execSQL("CREATE INDEX idx_messages_event ON $TABLE_MESSAGES(event_id)")

            db.execSQL(
                """
                CREATE TABLE $TABLE_MISSED_CALLS (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id TEXT,
                    number TEXT NOT NULL,
                    display_name TEXT NOT NULL DEFAULT '',
                    timestamp_ms INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_missed_ts ON $TABLE_MISSED_CALLS(timestamp_ms DESC, id DESC)")
            db.execSQL("CREATE INDEX idx_missed_event ON $TABLE_MISSED_CALLS(event_id)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
