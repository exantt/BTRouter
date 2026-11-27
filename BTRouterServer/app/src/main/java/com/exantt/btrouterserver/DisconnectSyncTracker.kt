package com.exantt.btrouterserver

/**
 * Отслеживает окно разрыва Bluetooth-связи и прогресс автоматической/ручной синхронизации
 * без дублирования уже доставленных живых СМС.
 */
class DisconnectSyncTracker {

    companion object {
        const val DEFAULT_MANUAL_FALLBACK_WINDOW_MS = 30 * 60 * 1000L
        const val ACTIVITY_LOOKBACK_CAP_MS = 4 * 60 * 1000L
        const val DISCONNECT_OVERLAP_MS = 5_000L
        const val LIVE_DEDUP_WINDOW_MS = 120_000L

        fun shouldDeduplicateLiveSms(
            dbSender: String,
            dbBody: String,
            dbDateMs: Long,
            liveSender: String,
            liveBody: String,
            liveTsMs: Long
        ): Boolean {
            return OutboundSmsQueue.phoneNumbersMatch(dbSender, liveSender) &&
                dbBody == liveBody &&
                kotlin.math.abs(dbDateMs - liveTsMs) <= LIVE_DEDUP_WINDOW_MS
        }
    }

    var pendingAutoSyncFromMs: Long = 0L
        private set
    var lastDisconnectTime: Long = 0L
        private set
    var lastSyncedSmsId: Long = 0L
        private set
    var lastSyncedCallId: Long = 0L
        private set

    private var collecting = false
    private val pendingEventIds = HashSet<String>()
    private var targetTimeMs = 0L
    private var hadMore = false

    fun restore(
        lastDisconnectTime: Long,
        pendingAutoSyncFromMs: Long,
        lastSyncedSmsId: Long,
        lastSyncedCallId: Long
    ) {
        this.lastDisconnectTime = maxOf(0L, lastDisconnectTime)
        this.pendingAutoSyncFromMs = maxOf(0L, pendingAutoSyncFromMs)
        this.lastSyncedSmsId = maxOf(0L, lastSyncedSmsId)
        this.lastSyncedCallId = maxOf(0L, lastSyncedCallId)
    }

    fun recordDisconnect(nowMs: Long, lastClientActivityMs: Long): Boolean {
        val safeNow = maxOf(0L, nowMs)
        val effectiveMs = if (
            lastClientActivityMs > 0L &&
            safeNow >= lastClientActivityMs &&
            (safeNow - lastClientActivityMs) <= ACTIVITY_LOOKBACK_CAP_MS
        ) {
            minOf(safeNow, lastClientActivityMs)
        } else {
            maxOf(0L, safeNow - DISCONNECT_OVERLAP_MS)
        }

        if (pendingAutoSyncFromMs == 0L) {
            pendingAutoSyncFromMs = effectiveMs
            lastDisconnectTime = effectiveMs
            return true
        }
        return false
    }

    fun autoSyncFetchFromTime(): Long? = pendingAutoSyncFromMs.takeIf { it > 0L }

    fun manualSyncFetchFromTime(nowMs: Long): Long {
        if (pendingAutoSyncFromMs > 0L) return pendingAutoSyncFromMs
        if (lastDisconnectTime > 0L) return lastDisconnectTime
        return maxOf(0L, nowMs - DEFAULT_MANUAL_FALLBACK_WINDOW_MS)
    }

    fun beginCollection(windowEndMs: Long) {
        pendingEventIds.clear()
        collecting = true
        targetTimeMs = windowEndMs
        hadMore = false
    }

    fun finishCollection(eventIds: Collection<String>, hasMore: Boolean): Boolean {
        pendingEventIds.addAll(eventIds)
        hadMore = hasMore
        collecting = false

        if (eventIds.isEmpty() && !hasMore) {
            lastDisconnectTime = targetTimeMs
            pendingAutoSyncFromMs = 0L
            targetTimeMs = 0L
            return true
        }
        return false
    }

    fun onEventAck(eventId: String): Boolean {
        if (!pendingEventIds.remove(eventId)) {
            return false
        }
        if (!collecting && pendingEventIds.isEmpty() && !hadMore && targetTimeMs > 0L) {
            lastDisconnectTime = targetTimeMs
            pendingAutoSyncFromMs = 0L
            targetTimeMs = 0L
            return true
        }
        return false
    }

    fun advanceSyncedSmsId(dbId: Long): Boolean {
        if (dbId > lastSyncedSmsId) {
            lastSyncedSmsId = dbId
            return true
        }
        return false
    }

    fun advanceSyncedCallId(dbId: Long): Boolean {
        if (dbId > lastSyncedCallId) {
            lastSyncedCallId = dbId
            return true
        }
        return false
    }
}
