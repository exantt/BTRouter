package com.exantt.btrouterclient

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Чистая бизнес-логика чатов, сортировки, дедупликации адресов и форматирования времени.
 */
object ChatLogic {

    const val MAX_PINNED_CHATS = 5

    const val STATUS_SENDING = 0
    const val STATUS_SENT = 1
    const val STATUS_FAILED = 2
    const val STATUS_UNKNOWN = 3
    const val STATUS_QUEUED = 4

    data class ChatThread(
        val chatKey: String,
        val address: String,
        val displayName: String,
        val isPinned: Boolean,
        val pinnedAt: Long,
        val isArchived: Boolean,
        val lastMessageBody: String,
        val lastMessageTs: Long,
        val lastMessageIncoming: Boolean,
        val unreadCount: Int
    )

    data class ChatMessage(
        val id: Long,
        val chatKey: String,
        val eventId: String?,
        val body: String,
        val timestampMs: Long,
        val isIncoming: Boolean,
        val isRead: Boolean,
        val status: Int,
        val otpCode: String?
    )

    data class MissedCallRecord(
        val id: Long,
        val eventId: String?,
        val number: String,
        val displayName: String,
        val timestampMs: Long
    )

    fun isPhoneLikeAddress(rawAddress: String): Boolean {
        val trimmed = rawAddress.trim()
        if (trimmed.isEmpty()) return false
        if (!trimmed.any { it in '0'..'9' }) return false
        return trimmed.all { ch ->
            ch in '0'..'9' || ch == '+' || ch == '-' || ch == '(' || ch == ')' || ch.isWhitespace()
        }
    }

    fun canonicalChatKey(rawAddress: String): String {
        val trimmed = rawAddress.trim()
        if (trimmed.isEmpty()) return "unknown"
        if (isPhoneLikeAddress(trimmed)) {
            val canonical = ContactsHelper.canonicalPhoneKey(trimmed)
            return canonical.ifEmpty { trimmed.lowercase(Locale.ROOT) }
        }
        return trimmed.lowercase(Locale.ROOT)
    }

    fun findMatchingPhoneChatKey(rawAddress: String, existingKeys: Collection<String>): String {
        val key = canonicalChatKey(rawAddress)
        if (key in existingKeys) return key
        if (!isPhoneLikeAddress(rawAddress)) return key

        val keyDigits = ContactsHelper.digitsOnly(key)
        if (keyDigits.length < 10) return key

        // Сначала ищем точное совпадение всех цифр (например +7900... vs 7900...)
        val exactDigitMatch = existingKeys.firstOrNull { candidate ->
            isPhoneLikeAddress(candidate) && ContactsHelper.digitsOnly(candidate) == keyDigits
        }
        if (exactDigitMatch != null) return exactDigitMatch

        // Затем ищем однозначное совпадение по последним 10 цифрам (например +7900... vs 8900... или 900...)
        val last10 = keyDigits.takeLast(10)
        val last10Matches = existingKeys.filter { candidate ->
            if (!isPhoneLikeAddress(candidate)) return@filter false
            val candDigits = ContactsHelper.digitsOnly(candidate)
            candDigits.length >= 10 && candDigits.takeLast(10) == last10
        }
        return if (last10Matches.size == 1) last10Matches[0] else key
    }

    fun isRepliableAddress(address: String): Boolean {
        return BtProtocol.sanitizeAndValidatePhone(address) != null
    }

    fun canPinMore(currentPinnedCount: Int): Boolean {
        return currentPinnedCount < MAX_PINNED_CHATS
    }

    fun sortActiveChats(chats: List<ChatThread>): List<ChatThread> {
        return chats.sortedWith { a, b ->
            if (a.isPinned != b.isPinned) {
                return@sortedWith if (a.isPinned) -1 else 1
            }
            val tsCmp = b.lastMessageTs.compareTo(a.lastMessageTs)
            if (tsCmp != 0) return@sortedWith tsCmp
            val pinCmp = b.pinnedAt.compareTo(a.pinnedAt)
            if (pinCmp != 0) return@sortedWith pinCmp
            a.chatKey.compareTo(b.chatKey)
        }
    }

    fun sortArchivedChats(chats: List<ChatThread>): List<ChatThread> {
        return chats.sortedWith { a, b ->
            val tsCmp = b.lastMessageTs.compareTo(a.lastMessageTs)
            if (tsCmp != 0) tsCmp else a.chatKey.compareTo(b.chatKey)
        }
    }

    fun avatarInitial(displayName: String, address: String): String? {
        val trimmedName = displayName.trim()
        if (trimmedName.isEmpty()) return null
        val trimmedAddr = address.trim()
        if (trimmedAddr.isNotEmpty() && trimmedName.equals(trimmedAddr, ignoreCase = true)) {
            return null
        }
        val first = trimmedName.firstOrNull { !it.isWhitespace() } ?: return null
        return first.toString().uppercase(Locale.ROOT)
    }

    fun avatarColorIndex(chatKey: String, paletteSize: Int): Int {
        if (paletteSize <= 0) return 0
        return (chatKey.hashCode() and 0x7fffffff) % paletteSize
    }

    fun formatChatListTime(timestampMs: Long, nowMs: Long, locale: Locale): String {
        if (timestampMs <= 0L) return ""
        val msgCal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }

        val msgDayEpoch = dayEpoch(msgCal)
        val nowDayEpoch = dayEpoch(nowCal)
        val dayDiff = nowDayEpoch - msgDayEpoch

        val pattern = when {
            dayDiff == 0L -> "HH:mm"
            dayDiff in 1L..6L -> "EEE"
            msgCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) -> {
                if (locale.language == "ru") "d MMM" else "MMM d"
            }
            else -> "dd.MM.yy"
        }
        return SimpleDateFormat(pattern, locale).format(Date(timestampMs))
    }

    private fun dayEpoch(cal: Calendar): Long {
        val copy = cal.clone() as Calendar
        copy.set(Calendar.HOUR_OF_DAY, 0)
        copy.set(Calendar.MINUTE, 0)
        copy.set(Calendar.SECOND, 0)
        copy.set(Calendar.MILLISECOND, 0)
        return Math.round(copy.timeInMillis / 86_400_000.0)
    }

    fun formatDayKey(timestampMs: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return year * 10000 + month * 100 + day
    }

    fun computeUnreadIdsToMarkRead(messages: List<ChatMessage>, lastVisibleMessageId: Long): List<Long> {
        if (lastVisibleMessageId <= 0L) return emptyList()
        return messages
            .filter { it.isIncoming && !it.isRead && it.id <= lastVisibleMessageId }
            .map { it.id }
    }

    fun resolveMessageTimestamp(
        rawTimestampMs: Long,
        receivedAtMs: Long,
        existingLastTs: Long,
        isSync: Boolean
    ): Long {
        if (isSync) {
            return if (rawTimestampMs > 0L) rawTimestampMs else receivedAtMs
        }
        val base = receivedAtMs
        return if (existingLastTs > 0L && base <= existingLastTs) {
            existingLastTs + 1L
        } else {
            base
        }
    }
}
