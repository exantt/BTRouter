package com.exantt.btrouterserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTriggerTrackerTest {

    @Test
    fun earlyDismissalWhenActualMatchesTargetClearsStateAndCancelsTimeout() {
        val tracker = RoutineTriggerTracker()
        assertFalse(tracker.isPending())

        assertEquals(-1, tracker.onNotificationPosted(notifId = 2100, targetState = 1))
        assertTrue(tracker.isPending())

        assertEquals(2100, tracker.dismissIfMatched(1))
        assertFalse(tracker.isPending())
        assertFalse(tracker.onTimeout(2100))
    }

    @Test
    fun mismatchingActualStateDoesNotDismissNotification() {
        val tracker = RoutineTriggerTracker()
        tracker.onNotificationPosted(notifId = 2101, targetState = 1)

        assertEquals(-1, tracker.dismissIfMatched(0))
        assertEquals(-1, tracker.dismissIfMatched(-1))
        assertTrue(tracker.isPending())
    }

    @Test
    fun supersedingTriggerReturnsPreviousIdAndIgnoresOldTimeout() {
        val tracker = RoutineTriggerTracker()
        assertEquals(-1, tracker.onNotificationPosted(notifId = 3100, targetState = 1))
        assertEquals(3100, tracker.onNotificationPosted(notifId = 3101, targetState = 0))

        assertTrue(tracker.isPending())
        assertFalse(tracker.onTimeout(3100))
        assertTrue(tracker.isPending())

        assertEquals(-1, tracker.dismissIfMatched(1))
        assertEquals(3101, tracker.dismissIfMatched(0))
        assertFalse(tracker.isPending())
    }

    @Test
    fun onTimeoutClearsMatchingNotifIdAndStopsPending() {
        val tracker = RoutineTriggerTracker()
        tracker.onNotificationPosted(notifId = 2105, targetState = 0)

        assertTrue(tracker.onTimeout(2105))
        assertFalse(tracker.isPending())
        assertEquals(-1, tracker.dismissIfMatched(0))
        assertFalse(tracker.onTimeout(2105))
    }

    @Test
    fun clearReturnsActiveNotifIdAndResetsState() {
        val tracker = RoutineTriggerTracker()
        assertEquals(-1, tracker.clear())

        tracker.onNotificationPosted(notifId = 3105, targetState = 1)
        assertEquals(3105, tracker.clear())
        assertFalse(tracker.isPending())
        assertEquals(-1, tracker.clear())
    }
}
