package com.exantt.btrouterserver

/**
 * Чистая сериализованная очередь исходящих СМС для модема Server.
 * Одновременно передаётся только одно СМС (все его части),
 * а перед запуском следующего выдерживается пауза, чтобы модем/RIL не терял команды.
 */
class OutboundSmsQueue(private val maxQueueSize: Int = MAX_QUEUE_SIZE) {

    companion object {
        const val MAX_QUEUE_SIZE = 30
        const val MSG_SENT_OK = "СМС доставлено в сеть оператора"
        const val MSG_SEND_FAILED = "Ошибка отправки СМС"
        const val MSG_MODEM_TIMEOUT = "Таймаут ответа модема Server"
    }

    data class OutboundSmsItem(
        val cmdId: String,
        val number: String,
        val parts: ArrayList<String>
    )

    data class EnqueueResult(
        val accepted: Boolean,
        val dispatchImmediately: Boolean
    )

    data class CompletionResult(
        val cmdId: String,
        val ok: Boolean,
        val message: String,
        val hasMoreQueued: Boolean
    )

    private var activeItem: OutboundSmsItem? = null
    private var activeCompletedParts = 0
    private var activeAllOk = true
    private var activeFailureReason: String? = null
    private var inCooldown = false
    private val queue = ArrayDeque<OutboundSmsItem>()

    fun enqueue(item: OutboundSmsItem): EnqueueResult {
        if (activeItem == null && !inCooldown) {
            setActive(item)
            return EnqueueResult(accepted = true, dispatchImmediately = true)
        }
        if (queue.size >= maxQueueSize) {
            return EnqueueResult(accepted = false, dispatchImmediately = false)
        }
        queue.addLast(item)
        return EnqueueResult(accepted = true, dispatchImmediately = false)
    }

    fun onPartResult(cmdId: String, errDesc: String?): CompletionResult? {
        val current = activeItem ?: return null
        if (current.cmdId != cmdId) return null

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

    fun onActiveFailure(cmdId: String, reason: String): CompletionResult? {
        val current = activeItem ?: return null
        if (current.cmdId != cmdId) return null

        clearActive()
        val hasMore = queue.isNotEmpty()
        inCooldown = hasMore
        return CompletionResult(cmdId = cmdId, ok = false, message = reason, hasMoreQueued = hasMore)
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
