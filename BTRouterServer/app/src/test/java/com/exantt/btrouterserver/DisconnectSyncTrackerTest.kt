package com.exantt.btrouterserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisconnectSyncTrackerTest {

    @Test
    fun firstConnectWithoutPriorDisconnectDoesNotTriggerAutoSync() {
        val tracker = DisconnectSyncTracker()
        tracker.restore(
            lastDisconnectTime = 0L,
            pendingAutoSyncFromMs = 0L,
            lastSyncedSmsId = 0L,
            lastSyncedCallId = 0L
        )
        assertNull(tracker.autoSyncFetchFromTime())
    }

    @Test
    fun disconnectUsesLastActivityAndPreservesEarliestUnsyncedTimestamp() {
        val tracker = DisconnectSyncTracker()

        // Недавняя активность клиента (30 с назад) -> старт окна от lastClientActivityMs
        val firstRecorded = tracker.recordDisconnect(nowMs = 1_000_000L, lastClientActivityMs = 970_000L)
        assertTrue(firstRecorded)
        assertEquals(970_000L, tracker.pendingAutoSyncFromMs)
        assertEquals(970_000L, tracker.lastDisconnectTime)
        assertEquals(970_000L, tracker.autoSyncFetchFromTime())

        // Повторный обрыв до завершения синхронизации НЕ перезатирает более раннюю метку
        val secondRecorded = tracker.recordDisconnect(nowMs = 1_200_000L, lastClientActivityMs = 1_195_000L)
        assertFalse(secondRecorded)
        assertEquals(970_000L, tracker.pendingAutoSyncFromMs)
        assertEquals(970_000L, tracker.autoSyncFetchFromTime())

        // После закрытия окна и нового длительного простоя (> 4 мин) берётся nowMs - 5с
        tracker.beginCollection(1_250_000L)
        assertTrue(tracker.finishCollection(emptyList(), hasMore = false))
        assertNull(tracker.autoSyncFetchFromTime())

        val staleActivityRecorded = tracker.recordDisconnect(
            nowMs = 2_000_000L,
            lastClientActivityMs = 2_000_000L - DisconnectSyncTracker.ACTIVITY_LOOKBACK_CAP_MS - 1_000L
        )
        assertTrue(staleActivityRecorded)
        assertEquals(2_000_000L - DisconnectSyncTracker.DISCONNECT_OVERLAP_MS, tracker.pendingAutoSyncFromMs)
    }

    @Test
    fun emptySyncClearsPendingAutoSyncAndAdvancesLastDisconnectTime() {
        val tracker = DisconnectSyncTracker()
        assertTrue(tracker.recordDisconnect(nowMs = 500_000L, lastClientActivityMs = 490_000L))
        assertEquals(490_000L, tracker.autoSyncFetchFromTime())

        tracker.beginCollection(windowEndMs = 600_000L)
        val shouldPersist = tracker.finishCollection(eventIds = emptyList(), hasMore = false)
        assertTrue(shouldPersist)
        assertNull(tracker.autoSyncFetchFromTime())
        assertEquals(0L, tracker.pendingAutoSyncFromMs)
        assertEquals(600_000L, tracker.lastDisconnectTime)
    }

    @Test
    fun nonEmptySyncClearsPendingAutoSyncOnlyAfterAllAcksArrive() {
        val tracker = DisconnectSyncTracker()
        assertTrue(tracker.recordDisconnect(nowMs = 500_000L, lastClientActivityMs = 495_000L))

        tracker.beginCollection(windowEndMs = 650_000L)
        assertFalse(tracker.finishCollection(eventIds = listOf("sms_db_10", "call_db_5"), hasMore = false))
        // Пока нет всех ACK, pendingAutoSyncFromMs сохраняется
        assertEquals(495_000L, tracker.autoSyncFetchFromTime())

        // Неизвестный ACK игнорируется
        assertFalse(tracker.onEventAck("unknown_event"))

        // Первый ACK ещё не завершает окно
        assertFalse(tracker.onEventAck("sms_db_10"))
        assertEquals(495_000L, tracker.autoSyncFetchFromTime())

        // Последний ACK закрывает окно и двигает lastDisconnectTime
        assertTrue(tracker.onEventAck("call_db_5"))
        assertNull(tracker.autoSyncFetchFromTime())
        assertEquals(650_000L, tracker.lastDisconnectTime)
    }

    @Test
    fun hasMoreKeepsPendingAutoSyncUntilComplete() {
        val tracker = DisconnectSyncTracker()
        assertTrue(tracker.recordDisconnect(nowMs = 100_000L, lastClientActivityMs = 95_000L))

        tracker.beginCollection(windowEndMs = 200_000L)
        assertFalse(tracker.finishCollection(eventIds = listOf("sms_db_1"), hasMore = true))
        // Даже после ACK первого батча hasMore = true не даёт закрыть окно
        assertFalse(tracker.onEventAck("sms_db_1"))
        assertEquals(95_000L, tracker.autoSyncFetchFromTime())

        // Следующий батч добирает хвост с hasMore = false
        tracker.beginCollection(windowEndMs = 250_000L)
        assertFalse(tracker.finishCollection(eventIds = listOf("sms_db_2"), hasMore = false))
        assertTrue(tracker.onEventAck("sms_db_2"))
        assertNull(tracker.autoSyncFetchFromTime())
        assertEquals(250_000L, tracker.lastDisconnectTime)
    }

    @Test
    fun manualSyncFallsBackTo30MinWindowWhenNeverDisconnected() {
        val tracker = DisconnectSyncTracker()
        val now = 5_000_000L
        assertEquals(
            now - DisconnectSyncTracker.DEFAULT_MANUAL_FALLBACK_WINDOW_MS,
            tracker.manualSyncFetchFromTime(now)
        )

        // После закрытого разрыва ручная синхронизация использует lastDisconnectTime
        tracker.recordDisconnect(nowMs = 6_000_000L, lastClientActivityMs = 5_990_000L)
        tracker.beginCollection(windowEndMs = 6_100_000L)
        assertTrue(tracker.finishCollection(emptyList(), hasMore = false))
        assertEquals(6_100_000L, tracker.manualSyncFetchFromTime(6_500_000L))

        // Если есть несинхронизированный разрыв, приоритет у pendingAutoSyncFromMs
        tracker.recordDisconnect(nowMs = 7_000_000L, lastClientActivityMs = 6_980_000L)
        assertEquals(6_980_000L, tracker.manualSyncFetchFromTime(7_100_000L))
    }

    @Test
    fun shouldDeduplicateLiveSmsMatchesPhoneVariantsAndTimeWindow() {
        assertTrue(
            DisconnectSyncTracker.shouldDeduplicateLiveSms(
                dbSender = "+7 (999) 111-22-33",
                dbBody = "Код 1234",
                dbDateMs = 1_000_000L,
                liveSender = "89991112233",
                liveBody = "Код 1234",
                liveTsMs = 1_060_000L
            )
        )
        // За пределами окна 120 с не склеивается (повторное одинаковое реальное СМС)
        assertFalse(
            DisconnectSyncTracker.shouldDeduplicateLiveSms(
                dbSender = "+79991112233",
                dbBody = "Код 1234",
                dbDateMs = 1_000_000L,
                liveSender = "+79991112233",
                liveBody = "Код 1234",
                liveTsMs = 1_120_001L
            )
        )
        // Разный текст или другой отправитель не склеиваются
        assertFalse(
            DisconnectSyncTracker.shouldDeduplicateLiveSms(
                dbSender = "+79991112233",
                dbBody = "Код 1234",
                dbDateMs = 1_000_000L,
                liveSender = "+79991112233",
                liveBody = "Код 5678",
                liveTsMs = 1_005_000L
            )
        )
        assertFalse(
            DisconnectSyncTracker.shouldDeduplicateLiveSms(
                dbSender = "+79991112233",
                dbBody = "Привет",
                dbDateMs = 1_000_000L,
                liveSender = "+79991112244",
                liveBody = "Привет",
                liveTsMs = 1_005_000L
            )
        )
    }

    @Test
    fun advanceSyncedIdsMoveMonotonicallyOnly() {
        val tracker = DisconnectSyncTracker()
        tracker.restore(0L, 0L, lastSyncedSmsId = 100L, lastSyncedCallId = 50L)

        assertFalse(tracker.advanceSyncedSmsId(90L))
        assertFalse(tracker.advanceSyncedSmsId(100L))
        assertEquals(100L, tracker.lastSyncedSmsId)
        assertTrue(tracker.advanceSyncedSmsId(105L))
        assertEquals(105L, tracker.lastSyncedSmsId)

        assertFalse(tracker.advanceSyncedCallId(40L))
        assertFalse(tracker.advanceSyncedCallId(50L))
        assertEquals(50L, tracker.lastSyncedCallId)
        assertTrue(tracker.advanceSyncedCallId(55L))
        assertEquals(55L, tracker.lastSyncedCallId)
    }
}
