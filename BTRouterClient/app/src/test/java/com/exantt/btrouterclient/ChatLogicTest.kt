package com.exantt.btrouterclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class ChatLogicTest {

    @Test
    fun canonicalChatKeyHandlesPhoneAndAlphanumericSenders() {
        assertEquals("+79991112233", ChatLogic.canonicalChatKey("+7 (999) 111-22-33"))
        assertEquals("900", ChatLogic.canonicalChatKey(" 900 "))
        assertEquals("t-bank", ChatLogic.canonicalChatKey("T-Bank"))
        assertEquals("mtc", ChatLogic.canonicalChatKey("MTC"))
        assertEquals("unknown", ChatLogic.canonicalChatKey("   "))
    }

    @Test
    fun findMatchingPhoneChatKeyMatchesFormattedAndLocalVariantsUnambiguously() {
        val existing = listOf("+79991112233", "+79994445566", "t-bank")
        assertEquals("+79991112233", ChatLogic.findMatchingPhoneChatKey("8 999 111-22-33", existing))
        assertEquals("+79991112233", ChatLogic.findMatchingPhoneChatKey("79991112233", existing))
        assertEquals("t-bank", ChatLogic.findMatchingPhoneChatKey("T-BANK", existing))
        assertEquals("+905551234567", ChatLogic.findMatchingPhoneChatKey("+90 555 123 45 67", existing))

        // Если в existing два разных кода страны с одинаковыми last10, слияния не происходит
        val ambiguous = listOf("+79991112233", "+19991112233")
        assertEquals("89991112233", ChatLogic.findMatchingPhoneChatKey("89991112233", ambiguous))
    }

    @Test
    fun sortActiveChatsPlacesPinnedFirstAndNewestFirstWithinGroups() {
        val c1 = ChatLogic.ChatThread("a", "+7001", "Alice", false, 0L, false, "hi", 5000L, true, 1)
        val c2 = ChatLogic.ChatThread("b", "+7002", "Bob", true, 100L, false, "pin old", 2000L, true, 0)
        val c3 = ChatLogic.ChatThread("c", "+7003", "Charlie", true, 200L, false, "pin new", 4000L, false, 0)
        val c4 = ChatLogic.ChatThread("d", "+7004", "Dave", false, 0L, false, "older", 3000L, true, 2)

        val sorted = ChatLogic.sortActiveChats(listOf(c1, c2, c3, c4))
        assertEquals(listOf("c", "b", "a", "d"), sorted.map { it.chatKey })
    }

    @Test
    fun canPinMoreEnforcesMaxFivePinnedChats() {
        assertTrue(ChatLogic.canPinMore(0))
        assertTrue(ChatLogic.canPinMore(4))
        assertFalse(ChatLogic.canPinMore(5))
        assertFalse(ChatLogic.canPinMore(6))
    }

    @Test
    fun avatarInitialReturnsFirstCharForNamedContactsAndNullForUnnamedOrSenderId() {
        assertEquals("Д", ChatLogic.avatarInitial("Дом", "+79990001122"))
        assertEquals("*", ChatLogic.avatarInitial("* Мама", "+79990001133"))
        assertEquals("A", ChatLogic.avatarInitial("alice", "+79990001144"))
        assertNull(ChatLogic.avatarInitial("", "900"))
        assertNull(ChatLogic.avatarInitial("T-Bank", "T-Bank"))
        assertNull(ChatLogic.avatarInitial("   ", "+79990001155"))
    }

    @Test
    fun isRepliableAddressValidatesPhoneNumbersOnly() {
        assertTrue(ChatLogic.isRepliableAddress("+7 (999) 123-45-67"))
        assertTrue(ChatLogic.isRepliableAddress("900"))
        assertFalse(ChatLogic.isRepliableAddress("T-Bank"))
        assertFalse(ChatLogic.isRepliableAddress("PTT"))
        assertFalse(ChatLogic.isRepliableAddress("Скрытый номер"))
    }

    @Test
    fun computeUnreadIdsToMarkReadSelectsIncomingUnreadUpToLastVisibleId() {
        val msgs = listOf(
            ChatLogic.ChatMessage(1L, "k", "e1", "m1", 100L, isIncoming = true, isRead = true, status = 1, otpCode = null),
            ChatLogic.ChatMessage(2L, "k", "e2", "m2", 200L, isIncoming = true, isRead = false, status = 1, otpCode = null),
            ChatLogic.ChatMessage(3L, "k", "e3", "m3", 300L, isIncoming = false, isRead = false, status = 1, otpCode = null),
            ChatLogic.ChatMessage(4L, "k", "e4", "m4", 400L, isIncoming = true, isRead = false, status = 1, otpCode = null),
            ChatLogic.ChatMessage(5L, "k", "e5", "m5", 500L, isIncoming = true, isRead = false, status = 1, otpCode = null)
        )
        assertEquals(listOf(2L, 4L), ChatLogic.computeUnreadIdsToMarkRead(msgs, 4L))
        assertEquals(emptyList<Long>(), ChatLogic.computeUnreadIdsToMarkRead(msgs, 1L))
        assertEquals(emptyList<Long>(), ChatLogic.computeUnreadIdsToMarkRead(msgs, 0L))
    }

    @Test
    fun formatChatListTimeHandlesSameDayWeekMonthAndYear() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 21, 18, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = nowCal.timeInMillis

        val sameDayMs = (nowCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 5)
        }.timeInMillis
        assertEquals("09:05", ChatLogic.formatChatListTime(sameDayMs, nowMs, Locale.US))

        val threeDaysAgoMs = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -3)
        }.timeInMillis
        val weekStr = ChatLogic.formatChatListTime(threeDaysAgoMs, nowMs, Locale.US)
        assertTrue(weekStr.isNotBlank() && !weekStr.contains(":"))

        val monthAgoMs = (nowCal.clone() as Calendar).apply {
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 22)
        }.timeInMillis
        val monthStr = ChatLogic.formatChatListTime(monthAgoMs, nowMs, Locale.US)
        assertTrue(monthStr.contains("22"))

        val prevYearMs = (nowCal.clone() as Calendar).apply {
            set(Calendar.YEAR, 2025)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 4)
        }.timeInMillis
        assertEquals("04.05.25", ChatLogic.formatChatListTime(prevYearMs, nowMs, Locale.US))
    }

    @Test
    fun resolveMessageTimestampKeepsLiveSequenceMonotonicAndPreservesSyncHistory() {
        val outgoingTs = 1_700_000_050_500L

        // Живое входящее СМС: время SMSC (rawTimestampMs) может отставать на несколько секунд от часов Т1
        val liveIncomingClockSkewTs = ChatLogic.resolveMessageTimestamp(
            rawTimestampMs = outgoingTs - 4_000L,
            receivedAtMs = outgoingTs - 1_500L,
            existingLastTs = outgoingTs,
            isSync = false
        )
        assertTrue(liveIncomingClockSkewTs > outgoingTs)
        assertEquals(outgoingTs + 1L, liveIncomingClockSkewTs)

        // Живое входящее с более поздним временем приёма на Т1 использует receivedAtMs
        val liveIncomingNormalTs = ChatLogic.resolveMessageTimestamp(
            rawTimestampMs = outgoingTs - 500L,
            receivedAtMs = outgoingTs + 2_300L,
            existingLastTs = outgoingTs,
            isSync = false
        )
        assertEquals(outgoingTs + 2_300L, liveIncomingNormalTs)

        // Новое исходящее, отправленное сразу после сообщения с slightly future ts, остаётся строго после него
        val quickOutgoingTs = ChatLogic.resolveMessageTimestamp(
            rawTimestampMs = outgoingTs - 10L,
            receivedAtMs = outgoingTs - 10L,
            existingLastTs = outgoingTs,
            isSync = false
        )
        assertEquals(outgoingTs + 1L, quickOutgoingTs)

        // Синхронизированная история сохраняет оригинальную метку времени
        val syncedHistoricalTs = ChatLogic.resolveMessageTimestamp(
            rawTimestampMs = outgoingTs - 120_000L,
            receivedAtMs = outgoingTs + 5_000L,
            existingLastTs = outgoingTs,
            isSync = true
        )
        assertEquals(outgoingTs - 120_000L, syncedHistoricalTs)

        val syncedFallbackTs = ChatLogic.resolveMessageTimestamp(
            rawTimestampMs = 0L,
            receivedAtMs = outgoingTs + 5_000L,
            existingLastTs = outgoingTs,
            isSync = true
        )
        assertEquals(outgoingTs + 5_000L, syncedFallbackTs)
    }

    @Test
    fun canTransitionOutgoingStatusProtectsTerminalStatesAndAllowsLateConfirmation() {
        // Идентичный статус не даёт лишнего обновления
        assertFalse(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENDING, ChatLogic.STATUS_SENDING))
        assertFalse(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENT, ChatLogic.STATUS_SENT))

        // Нормальные переходы из SENDING / QUEUED / UNKNOWN
        assertTrue(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENDING, ChatLogic.STATUS_SENT))
        assertTrue(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENDING, ChatLogic.STATUS_FAILED))
        assertTrue(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENDING, ChatLogic.STATUS_UNKNOWN))
        assertTrue(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_QUEUED, ChatLogic.STATUS_SENDING))
        assertTrue(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_UNKNOWN, ChatLogic.STATUS_SENT))
        assertTrue(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_UNKNOWN, ChatLogic.STATUS_FAILED))

        // Подтверждённое STATUS_SENT защищено от откатов (например, из-за обрыва BT после DB-фолбека)
        assertFalse(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENT, ChatLogic.STATUS_FAILED))
        assertFalse(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENT, ChatLogic.STATUS_UNKNOWN))
        assertFalse(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_SENT, ChatLogic.STATUS_SENDING))

        // Из STATUS_FAILED разрешено повышение до STATUS_SENT (поздний callback), но не откат в UNKNOWN
        assertTrue(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_FAILED, ChatLogic.STATUS_SENT))
        assertFalse(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_FAILED, ChatLogic.STATUS_UNKNOWN))
        assertFalse(ChatLogic.canTransitionOutgoingStatus(ChatLogic.STATUS_FAILED, ChatLogic.STATUS_SENDING))
    }

    @Test
    fun internationalNumbersWithDifferentCountryCodesDoNotMerge() {
        assertEquals(
            "+19991112233",
            ChatLogic.findMatchingPhoneChatKey("+19991112233", listOf("+79991112233"))
        )
        assertEquals(
            "+79991112233",
            ChatLogic.findMatchingPhoneChatKey("89991112233", listOf("+79991112233"))
        )
    }

    @Test
    fun delayedLiveMessageOverSixtySecondsPreservesRawTimestamp() {
        val rawTs = 1_700_000_000_000L
        val receivedTs = rawTs + 120_000L
        val resolved = ChatLogic.resolveMessageTimestamp(
            rawTimestampMs = rawTs,
            receivedAtMs = receivedTs,
            existingLastTs = rawTs - 10_000L,
            isSync = false
        )
        assertEquals(rawTs, resolved)
    }
}
