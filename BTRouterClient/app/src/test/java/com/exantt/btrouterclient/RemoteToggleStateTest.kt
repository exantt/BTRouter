package com.exantt.btrouterclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteToggleStateTest {

    @Test
    fun confirmsWhenMatchingStatusArrivesWithinWindow() {
        val state = RemoteToggleState()
        assertEquals(-1, state.applyStatus(0, nowMs = 1_000L).snapshot.pendingTarget)
        assertEquals(0, state.actualState)

        val dispatched = state.onDispatched(target = 1, nowMs = 2_000L, pendingWindowMs = 8_000L)
        assertEquals(1, dispatched.pendingTarget)
        assertEquals(1, dispatched.requestedState)
        assertEquals(0, dispatched.actualState)

        val midResult = state.applyStatus(1, nowMs = 5_000L)
        assertTrue(midResult.confirmed)
        assertFalse(midResult.timedOutUnchanged)
        assertEquals(-1, midResult.snapshot.pendingTarget)
        assertEquals(1, midResult.snapshot.actualState)

        val timeoutAfterConfirm = state.onTimeout()
        assertFalse(timeoutAfterConfirm.timedOutUnchanged)
    }

    @Test
    fun keepsPendingActiveWhileUnchangedStatusArrivesInsideWindow() {
        val state = RemoteToggleState()
        state.applyStatus(0, nowMs = 1_000L)
        state.onDispatched(target = 1, nowMs = 2_000L, pendingWindowMs = 8_000L)

        val res = state.applyStatus(0, nowMs = 7_500L)
        assertFalse(res.confirmed)
        assertFalse(res.timedOutUnchanged)
        assertEquals(1, res.snapshot.pendingTarget)
        assertEquals(1, res.snapshot.requestedState)
        assertEquals(0, res.snapshot.actualState)
    }

    @Test
    fun onTimeoutReportsFailureAndPreservesActualStateWhenUnchanged() {
        val state = RemoteToggleState()
        state.applyStatus(0, nowMs = 1_000L)
        state.onDispatched(target = 1, nowMs = 2_000L, pendingWindowMs = 8_000L)

        val timeout = state.onTimeout()
        assertTrue(timeout.timedOutUnchanged)
        assertEquals(1, timeout.failedTarget)
        assertEquals(0, timeout.snapshot.actualState)
        assertEquals(-1, timeout.snapshot.pendingTarget)
        assertEquals(-1, timeout.snapshot.requestedState)
    }

    @Test
    fun onTimeoutReportsFailureWhenActualStateUnknown() {
        val state = RemoteToggleState()
        state.onDispatched(target = 0, nowMs = 2_000L, pendingWindowMs = 8_000L)

        val timeout = state.onTimeout()
        assertTrue(timeout.timedOutUnchanged)
        assertEquals(0, timeout.failedTarget)
        assertEquals(-1, timeout.snapshot.actualState)
        assertEquals(-1, timeout.snapshot.pendingTarget)
        assertEquals(-1, timeout.snapshot.requestedState)
    }

    @Test
    fun lateUnchangedStatusAfterWindowExpiresTriggersWarningOnce() {
        val state = RemoteToggleState()
        state.applyStatus(0, nowMs = 1_000L)
        state.onDispatched(target = 1, nowMs = 2_000L, pendingWindowMs = 8_000L)

        val lateStatus = state.applyStatus(0, nowMs = 10_050L)
        assertFalse(lateStatus.confirmed)
        assertTrue(lateStatus.timedOutUnchanged)
        assertEquals(1, lateStatus.failedTarget)
        assertEquals(-1, lateStatus.snapshot.pendingTarget)
        assertEquals(-1, lateStatus.snapshot.requestedState)
        assertEquals(0, lateStatus.snapshot.actualState)

        // Последующий onTimeout больше не должен повторно показывать ошибку
        val duplicateTimeout = state.onTimeout()
        assertFalse(duplicateTimeout.timedOutUnchanged)
        assertEquals(-1, duplicateTimeout.failedTarget)
    }

    @Test
    fun ackFailedAndResetClearPendingAndRequestedStatesCleanly() {
        val state = RemoteToggleState()
        state.applyStatus(1, nowMs = 1_000L)
        state.onDispatched(target = 0, nowMs = 2_000L, pendingWindowMs = 8_000L)

        val ackFailedSnap = state.onAckFailed()
        assertEquals(1, ackFailedSnap.actualState)
        assertEquals(-1, ackFailedSnap.pendingTarget)
        assertEquals(-1, ackFailedSnap.requestedState)

        val resetSnap = state.reset()
        assertEquals(-1, resetSnap.actualState)
        assertEquals(-1, resetSnap.pendingTarget)
        assertEquals(-1, resetSnap.requestedState)
    }
}
