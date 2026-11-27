package com.exantt.btrouterclient

/**
 * Чистая конечная машина состояния удалённого переключателя (Hotspot / Mobile Data) на Т1:
 * отслеживает фактическое состояние, ожидаемую цель и факт истечения окна подтверждения.
 */
class RemoteToggleState {

    data class Snapshot(
        val actualState: Int,
        val pendingTarget: Int,
        val requestedState: Int
    )

    data class StatusResult(
        val snapshot: Snapshot,
        val confirmed: Boolean,
        val timedOutUnchanged: Boolean,
        val failedTarget: Int
    )

    data class TimeoutResult(
        val snapshot: Snapshot,
        val timedOutUnchanged: Boolean,
        val failedTarget: Int
    )

    @Volatile
    var actualState: Int = -1
        private set
    @Volatile
    var requestedState: Int = -1
        private set
    @Volatile
    var pendingTarget: Int = -1
        private set
    @Volatile
    var pendingUntilMs: Long = 0L
        private set

    fun snapshot(): Snapshot = Snapshot(actualState, pendingTarget, requestedState)

    fun onDispatched(target: Int, nowMs: Long, pendingWindowMs: Long): Snapshot {
        requestedState = target
        pendingTarget = target
        pendingUntilMs = nowMs + pendingWindowMs
        return snapshot()
    }

    fun applyStatus(rawValue: Int, nowMs: Long): StatusResult {
        val normalized = if (rawValue == 0 || rawValue == 1) rawValue else -1
        actualState = normalized
        if (pendingTarget != -1) {
            if (normalized == pendingTarget) {
                pendingTarget = -1
                pendingUntilMs = 0L
                return StatusResult(
                    snapshot = snapshot(),
                    confirmed = true,
                    timedOutUnchanged = false,
                    failedTarget = -1
                )
            } else if (nowMs >= pendingUntilMs) {
                val failed = pendingTarget
                pendingTarget = -1
                pendingUntilMs = 0L
                requestedState = -1
                return StatusResult(
                    snapshot = snapshot(),
                    confirmed = false,
                    timedOutUnchanged = true,
                    failedTarget = failed
                )
            }
        }
        return StatusResult(
            snapshot = snapshot(),
            confirmed = false,
            timedOutUnchanged = false,
            failedTarget = -1
        )
    }

    fun onTimeout(): TimeoutResult {
        if (pendingTarget == -1) {
            return TimeoutResult(
                snapshot = snapshot(),
                timedOutUnchanged = false,
                failedTarget = -1
            )
        }
        val failed = pendingTarget
        pendingTarget = -1
        pendingUntilMs = 0L
        if (actualState == failed) {
            return TimeoutResult(
                snapshot = snapshot(),
                timedOutUnchanged = false,
                failedTarget = -1
            )
        }
        requestedState = -1
        return TimeoutResult(
            snapshot = snapshot(),
            timedOutUnchanged = true,
            failedTarget = failed
        )
    }

    fun onAckFailed(): Snapshot {
        pendingTarget = -1
        pendingUntilMs = 0L
        requestedState = -1
        return snapshot()
    }

    fun reset(): Snapshot {
        actualState = -1
        requestedState = -1
        pendingTarget = -1
        pendingUntilMs = 0L
        return snapshot()
    }
}
