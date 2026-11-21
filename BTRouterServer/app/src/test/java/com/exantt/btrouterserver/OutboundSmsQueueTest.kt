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
    }
}
