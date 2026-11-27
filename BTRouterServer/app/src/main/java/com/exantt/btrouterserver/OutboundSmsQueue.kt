package com.exantt.btrouterserver

/**
 * Чистая сериализованная очередь исходящих СМС для модема Server.
 * Одновременно передаётся только одно СМС (все его части),
 * а перед запуском следующего выдерживается пауза, чтобы модем/RIL не терял команды.
 */
class OutboundSmsQueue(private val maxQueueSize: Int = MAX_QUEUE_SIZE) {

    companion object {
        const val MAX_QUEUE_SIZE = 30
        const val MAX_RECENT_CMD_IDS = 100
        const val MAX_TIMED_OUT_PENDING = 20
        const val TIMED_OUT_TRACK_TTL_MS = 5 * 60_000L
        const val MSG_SENT_OK = "СМС доставлено в сеть оператора"
        const val MSG_SEND_FAILED = "Ошибка отправки СМС"
        const val MSG_MODEM_TIMEOUT = "Таймаут ответа модема Server"

        fun phoneNumbersMatch(a: String, b: String): Boolean {
            val digitsA = extractDigits(a)
            val digitsB = extractDigits(b)
            if (digitsA.isEmpty() || digitsB.isEmpty()) {
                return a.trim().equals(b.trim(), ignoreCase = true)
            }
            if (digitsA == digitsB) return true
            return digitsA.length >= 10 && digitsB.length >= 10 && digitsA.takeLast(10) == digitsB.takeLast(10)
        }

        private fun extractDigits(raw: String): String {
            val sb = StringBuilder(raw.length)
            for (ch in raw) {
                if (ch in '0'..'9') sb.append(ch)
            }
            return sb.toString()
        }
    }

    data class OutboundSmsItem(
        val cmdId: String,
        val number: String,
        val parts: ArrayList<String>,
        val fullBody: String = parts.joinToString("")
    )

    data class EnqueueResult(
        val accepted: Boolean,
        val dispatchImmediately: Boolean,
        val duplicate: Boolean = false
    )

    data class CompletionResult(
        val cmdId: String,
        val ok: Boolean,
        val message: String,
        val hasMoreQueued: Boolean,
        val timedOut: Boolean = false
    )

    private data class TimedOutSms(
        val cmdId: String,
        val totalParts: Int,
        var completedParts: Int,
        var allOk: Boolean,
        var failureReason: String?,
        val timedOutAtMs: Long
    )

    private var activeItem: OutboundSmsItem? = null
    private var activeCompletedParts = 0
    private var activeAllOk = true
    private var activeFailureReason: String? = null
    private var inCooldown = false
    private val queue = ArrayDeque<OutboundSmsItem>()
    private val recentCmdIds = LinkedHashSet<String>()
    private val timedOutPending = LinkedHashMap<String, TimedOutSms>()

    fun activeCmdId(): String? = activeItem?.cmdId

    fun enqueue(item: OutboundSmsItem): EnqueueResult {
        if (item.cmdId.isNotEmpty() && item.cmdId in recentCmdIds) {
            return EnqueueResult(accepted = false, dispatchImmediately = false, duplicate = true)
        }
        if (activeItem == null && !inCooldown) {
            recordCmdId(item.cmdId)
            setActive(item)
            return EnqueueResult(accepted = true, dispatchImmediately = true)
        }
        if (queue.size >= maxQueueSize) {
            return EnqueueResult(accepted = false, dispatchImmediately = false)
        }
        recordCmdId(item.cmdId)
        queue.addLast(item)
        return EnqueueResult(accepted = true, dispatchImmediately = false)
    }

    fun onPartResult(
        cmdId: String,
        errDesc: String?,
        nowMs: Long = System.currentTimeMillis()
    ): CompletionResult? {
        val current = activeItem
        if (current != null && current.cmdId == cmdId) {
            activeCompletedParts++
            if (errDesc != null) {
                activeAllOk = false
                if (activeFailureReason == null) {
                    activeFailureReason = errDesc
                }
            }

            if (activeCompletedParts < current.parts.size) {
                return null
            }

            val ok = activeAllOk
            val msg = if (ok) MSG_SENT_OK else (activeFailureReason ?: MSG_SEND_FAILED)
            clearActive()
            val hasMore = queue.isNotEmpty()
            inCooldown = hasMore
            return CompletionResult(cmdId = cmdId, ok = ok, message = msg, hasMoreQueued = hasMore)
        }

        pruneExpiredTimedOut(nowMs)
        val late = timedOutPending[cmdId] ?: return null
        late.completedParts++
        if (errDesc != null) {
            late.allOk = false
            if (late.failureReason == null) {
                late.failureReason = errDesc
            }
        }
        if (late.completedParts < late.totalParts) {
            return null
        }

        timedOutPending.remove(cmdId)
        val ok = late.allOk
        val msg = if (ok) MSG_SENT_OK else (late.failureReason ?: MSG_SEND_FAILED)
        return CompletionResult(
            cmdId = cmdId,
            ok = ok,
            message = msg,
            hasMoreQueued = false,
            timedOut = false
        )
    }

    fun onActiveTimeout(
        cmdId: String,
        nowMs: Long = System.currentTimeMillis()
    ): CompletionResult? {
        val current = activeItem ?: return null
        if (current.cmdId != cmdId) return null

        pruneExpiredTimedOut(nowMs)
        while (timedOutPending.size >= MAX_TIMED_OUT_PENDING) {
            val oldest = timedOutPending.keys.firstOrNull() ?: break
            timedOutPending.remove(oldest)
        }
        timedOutPending[cmdId] = TimedOutSms(
            cmdId = cmdId,
            totalParts = current.parts.size,
            completedParts = activeCompletedParts,
            allOk = activeAllOk,
            failureReason = activeFailureReason,
            timedOutAtMs = nowMs
        )

        clearActive()
        val hasMore = queue.isNotEmpty()
        inCooldown = hasMore
        return CompletionResult(
            cmdId = cmdId,
            ok = false,
            message = MSG_MODEM_TIMEOUT,
            hasMoreQueued = hasMore,
            timedOut = true
        )
    }

    fun onActiveFailure(cmdId: String, reason: String): CompletionResult? {
        val current = activeItem ?: return null
        if (current.cmdId != cmdId) return null

        clearActive()
        val hasMore = queue.isNotEmpty()
        inCooldown = hasMore
        return CompletionResult(cmdId = cmdId, ok = false, message = reason, hasMoreQueued = hasMore)
    }

    fun onActiveDbFallbackResult(cmdId: String, ok: Boolean, reason: String? = null): CompletionResult? {
        val current = activeItem ?: return null
        if (current.cmdId != cmdId) return null

        clearActive()
        val hasMore = queue.isNotEmpty()
        inCooldown = hasMore
        val msg = if (ok) MSG_SENT_OK else (reason ?: MSG_SEND_FAILED)
        return CompletionResult(
            cmdId = cmdId,
            ok = ok,
            message = msg,
            hasMoreQueued = hasMore,
            timedOut = false
        )
    }

    fun onCooldownFinished(): OutboundSmsItem? {
        inCooldown = false
        if (activeItem != null) return null
        val next = queue.removeFirstOrNull() ?: return null
        setActive(next)
        return next
    }

    fun clear() {
        clearActive()
        inCooldown = false
        queue.clear()
        timedOutPending.clear()
        recentCmdIds.clear()
    }

    private fun recordCmdId(cmdId: String) {
        if (cmdId.isEmpty()) return
        while (recentCmdIds.size >= MAX_RECENT_CMD_IDS) {
            val oldest = recentCmdIds.firstOrNull() ?: break
            recentCmdIds.remove(oldest)
        }
        recentCmdIds.add(cmdId)
    }

    private fun pruneExpiredTimedOut(nowMs: Long) {
        val iter = timedOutPending.entries.iterator()
        while (iter.hasNext()) {
            if (nowMs - iter.next().value.timedOutAtMs > TIMED_OUT_TRACK_TTL_MS) {
                iter.remove()
            }
        }
    }

    private fun setActive(item: OutboundSmsItem) {
        activeItem = item
        activeCompletedParts = 0
        activeAllOk = true
        activeFailureReason = null
    }

    private fun clearActive() {
        activeItem = null
        activeCompletedParts = 0
        activeAllOk = true
        activeFailureReason = null
    }
}
