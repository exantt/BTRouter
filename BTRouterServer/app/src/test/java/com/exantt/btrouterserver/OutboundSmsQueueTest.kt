package com.exantt.btrouterserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboundSmsQueueTest {

    @Test
    fun singleSmsDispatchesImmediatelyAndCompletesWithoutMoreQueued() {
        val q = OutboundSmsQueue()
        val res = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-1", "+79990001122", arrayListOf("Hi")))
        assertTrue(res.accepted)
        assertTrue(res.dispatchImmediately)

        val done = q.onPartResult("c-1", null)
        assertNotNull(done)
        assertEquals("c-1", done!!.cmdId)
        assertTrue(done.ok)
        assertEquals(OutboundSmsQueue.MSG_SENT_OK, done.message)
        assertFalse(done.hasMoreQueued)
        assertNull(q.onCooldownFinished())
    }

    @Test
    fun threeRapidSmsSerializeStrictlyThroughCooldownWindow() {
        val q = OutboundSmsQueue()
        val r1 = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-1", "+7001", arrayListOf("1")))
        val r2 = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-2", "+7001", arrayListOf("2")))
        val r3 = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-3", "+7001", arrayListOf("3")))

        assertTrue(r1.accepted && r1.dispatchImmediately)
        assertTrue(r2.accepted && !r2.dispatchImmediately)
        assertTrue(r3.accepted && !r3.dispatchImmediately)

        // Завершается #1 -> сигнализируем наличие очереди и запускаем cooldown
        val d1 = q.onPartResult("c-1", null)!!
        assertTrue(d1.ok)
        assertTrue(d1.hasMoreQueued)

        // Если во время cooldown приходит #4, оно не перескакивает вперёд #2/#3
        val r4 = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-4", "+7001", arrayListOf("4")))
        assertTrue(r4.accepted && !r4.dispatchImmediately)

        // По окончании cooldown достаётся #2
        val next2 = q.onCooldownFinished()
        assertEquals("c-2", next2?.cmdId)
        val d2 = q.onPartResult("c-2", null)!!
        assertTrue(d2.hasMoreQueued)

        val next3 = q.onCooldownFinished()
        assertEquals("c-3", next3?.cmdId)
        val d3 = q.onPartResult("c-3", null)!!
        assertTrue(d3.hasMoreQueued)

        val next4 = q.onCooldownFinished()
        assertEquals("c-4", next4?.cmdId)
        val d4 = q.onPartResult("c-4", null)!!
        assertFalse(d4.hasMoreQueued)
        assertNull(q.onCooldownFinished())
    }

    @Test
    fun multipartSmsWaitsForAllPartsAndReportsFirstError() {
        val q = OutboundSmsQueue()
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-multi", "+7001", arrayListOf("p1", "p2", "p3")))

        assertNull(q.onPartResult("c-multi", null))
        assertNull(q.onPartResult("c-multi", "Нет сотовой сети на Server"))
        val done = q.onPartResult("c-multi", "Сбой отправки оператором")
        assertNotNull(done)
        assertFalse(done!!.ok)
        assertEquals("Нет сотовой сети на Server", done.message)
        assertFalse(done.hasMoreQueued)
    }

    @Test
    fun activeFailureAdvancesToQueuedSmsAndIgnoresLateBroadcastForTimedOutCmd() {
        val q = OutboundSmsQueue()
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-timeout", "+7001", arrayListOf("slow")))
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-next", "+7001", arrayListOf("ok")))

        val fail = q.onActiveFailure("c-timeout", OutboundSmsQueue.MSG_MODEM_TIMEOUT)
        assertNotNull(fail)
        assertFalse(fail!!.ok)
        assertEquals(OutboundSmsQueue.MSG_MODEM_TIMEOUT, fail.message)
        assertTrue(fail.hasMoreQueued)

        // Поздний broadcast по уже отвалившемуся cmdId игнорируется и не ломает #2
        assertNull(q.onPartResult("c-timeout", null))

        val next = q.onCooldownFinished()
        assertEquals("c-next", next?.cmdId)
        assertNull(q.onPartResult("c-timeout", null))

        val doneNext = q.onPartResult("c-next", null)!!
        assertEquals("c-next", doneNext.cmdId)
        assertTrue(doneNext.ok)
        assertFalse(doneNext.hasMoreQueued)
    }

    @Test
    fun enqueueRejectsItemsBeyondMaxQueueSize() {
        val q = OutboundSmsQueue(maxQueueSize = 2)
        assertTrue(q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-1", "+7001", arrayListOf("1"))).dispatchImmediately)
        assertTrue(q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-2", "+7001", arrayListOf("2"))).accepted)
        assertTrue(q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-3", "+7001", arrayListOf("3"))).accepted)
        val rejected = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-4", "+7001", arrayListOf("4")))
        assertFalse(rejected.accepted)
        assertFalse(rejected.dispatchImmediately)
        assertFalse(rejected.duplicate)
    }

    @Test
    fun duplicateCmdIdIsRejectedWithoutDispatchingTwice() {
        val q = OutboundSmsQueue()
        val first = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-dup", "+7001", arrayListOf("hello")))
        assertTrue(first.accepted && first.dispatchImmediately && !first.duplicate)

        // Пока активно и после завершения тот же cmdId отклоняется как дубликат
        val whileActive = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-dup", "+7001", arrayListOf("hello")))
        assertFalse(whileActive.accepted)
        assertFalse(whileActive.dispatchImmediately)
        assertTrue(whileActive.duplicate)

        assertNotNull(q.onPartResult("c-dup", null))

        val afterDone = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-dup", "+7001", arrayListOf("hello")))
        assertFalse(afterDone.accepted)
        assertFalse(afterDone.dispatchImmediately)
        assertTrue(afterDone.duplicate)
    }

    @Test
    fun timeoutMarksTimedOutAndLateSentOkCompletesWithoutTriggeringQueueAgain() {
        val q = OutboundSmsQueue()
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-1", "+7001", arrayListOf("first")))
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-2", "+7001", arrayListOf("second")))

        val timedOut = q.onActiveTimeout("c-1", nowMs = 1_000L)
        assertNotNull(timedOut)
        assertFalse(timedOut!!.ok)
        assertTrue(timedOut.timedOut)
        assertEquals(OutboundSmsQueue.MSG_MODEM_TIMEOUT, timedOut.message)
        assertTrue(timedOut.hasMoreQueued)

        // Запускаем #2 после cooldown, пока #1 в timedOutPending
        val next = q.onCooldownFinished()
        assertEquals("c-2", next?.cmdId)
        assertEquals("c-2", q.activeCmdId())

        // Приходит поздний RESULT_OK по #1: подтверждает #1, НЕ трогает активное #2 и не перезапускает очередь
        val lateOk = q.onPartResult("c-1", null, nowMs = 2_000L)
        assertNotNull(lateOk)
        assertEquals("c-1", lateOk!!.cmdId)
        assertTrue(lateOk.ok)
        assertFalse(lateOk.timedOut)
        assertFalse(lateOk.hasMoreQueued)
        assertEquals("c-2", q.activeCmdId())

        // После завершения #1 повторный broadcast игнорируется
        assertNull(q.onPartResult("c-1", null, nowMs = 3_000L))

        val done2 = q.onPartResult("c-2", null, nowMs = 4_000L)
        assertNotNull(done2)
        assertTrue(done2!!.ok)
        assertFalse(done2.hasMoreQueued)
    }

    @Test
    fun timeoutWithLateErrorCompletesAsFailed() {
        val q = OutboundSmsQueue()
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-err", "+7001", arrayListOf("p1", "p2")))

        val timedOut = q.onActiveTimeout("c-err", nowMs = 1_000L)!!
        assertTrue(timedOut.timedOut)
        assertFalse(timedOut.hasMoreQueued)

        // 1-я часть ок, 2-я часть с ошибкой
        assertNull(q.onPartResult("c-err", null, nowMs = 2_000L))
        val lateFail = q.onPartResult("c-err", "Сбой отправки оператором", nowMs = 3_000L)
        assertNotNull(lateFail)
        assertFalse(lateFail!!.ok)
        assertFalse(lateFail.timedOut)
        assertFalse(lateFail.hasMoreQueued)
        assertEquals("Сбой отправки оператором", lateFail.message)
    }

    @Test
    fun lateBroadcastForTimedOutCmdDoesNotAffectNewActiveSms() {
        val q = OutboundSmsQueue()
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-1", "+7001", arrayListOf("slow")))
        val timedOut = q.onActiveTimeout("c-1", nowMs = 1_000L)!!
        assertTrue(timedOut.timedOut)

        // Следующее СМС сразу становится активным
        val second = q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-2", "+7001", arrayListOf("fast")))
        assertTrue(second.dispatchImmediately)
        assertEquals("c-2", q.activeCmdId())

        // Поздний callback по #1 завершает только #1, #2 остаётся активным и завершается отдельно
        val late1 = q.onPartResult("c-1", null, nowMs = 2_000L)!!
        assertEquals("c-1", late1.cmdId)
        assertTrue(late1.ok)
        assertEquals("c-2", q.activeCmdId())

        val done2 = q.onPartResult("c-2", null, nowMs = 2_500L)!!
        assertEquals("c-2", done2.cmdId)
        assertTrue(done2.ok)
        assertNull(q.activeCmdId())
    }

    @Test
    fun dbFallbackCompletesActiveAndSuppressesSubsequentLateBroadcast() {
        val q = OutboundSmsQueue()
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-fb", "+7001", arrayListOf("msg")))
        q.enqueue(OutboundSmsQueue.OutboundSmsItem("c-next", "+7001", arrayListOf("next")))

        val fbDone = q.onActiveDbFallbackResult("c-fb", ok = true)
        assertNotNull(fbDone)
        assertEquals("c-fb", fbDone!!.cmdId)
        assertTrue(fbDone.ok)
        assertFalse(fbDone.timedOut)
        assertTrue(fbDone.hasMoreQueued)
        assertNull(q.activeCmdId())

        // Поздний sentIntent для c-fb уже не должен срабатывать (не добавляется в timedOutPending)
        assertNull(q.onPartResult("c-fb", null))

        val next = q.onCooldownFinished()
        assertEquals("c-next", next?.cmdId)
    }

    @Test
    fun phoneNumbersMatchHandlesFormattingAndCountryCodeVariants() {
        assertTrue(OutboundSmsQueue.phoneNumbersMatch("+7 (999) 111-22-33", "+79991112233"))
        assertTrue(OutboundSmsQueue.phoneNumbersMatch("89991112233", "+79991112233"))
        assertTrue(OutboundSmsQueue.phoneNumbersMatch("+90 536 431 68 64", "05364316864"))
        assertTrue(OutboundSmsQueue.phoneNumbersMatch("900", " 900 "))
        assertFalse(OutboundSmsQueue.phoneNumbersMatch("+79991112233", "+79991112234"))
        assertFalse(OutboundSmsQueue.phoneNumbersMatch("12345", "67890"))
    }
}
