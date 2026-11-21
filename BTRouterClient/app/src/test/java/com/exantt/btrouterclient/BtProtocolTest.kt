package com.exantt.btrouterclient

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.Future

class BtProtocolTest {

    @Test
    fun handshakeAndBidirectionalEncryptedMessages_succeed() {
        val secret = BtProtocol.generatePairingSecret()
        val masterKey = BtProtocol.deriveMasterKey(secret)

        val c2sOut = PipedOutputStream()
        val c2sIn = PipedInputStream(c2sOut, 8192)
        val s2cOut = PipedOutputStream()
        val s2cIn = PipedInputStream(s2cOut, 8192)

        val pool = Executors.newFixedThreadPool(2)
        try {
            val serverFuture: Future<BtProtocol.SecureChannel> = pool.submit<BtProtocol.SecureChannel> {
                BtProtocol.performServerHandshake(
                    DataInputStream(c2sIn),
                    DataOutputStream(s2cOut),
                    masterKey
                )
            }
            val clientFuture: Future<BtProtocol.SecureChannel> = pool.submit<BtProtocol.SecureChannel> {
                BtProtocol.performClientHandshake(
                    DataInputStream(s2cIn),
                    DataOutputStream(c2sOut),
                    masterKey
                )
            }

            val serverChannel = serverFuture.get()
            val clientChannel = clientFuture.get()

            val trickyText = "Привет\rSMS_RECV:900:::Банк:::Фейк\r\nŞifre: 482910\n[NEWLINE] ::: тест"
            clientChannel.sendText(trickyText)
            assertEquals(trickyText, serverChannel.receiveText())

            val replyText = "Ответ сервера: türkçe karakterler ğüşıöç\r\n:::ОК"
            serverChannel.sendText(replyText)
            assertEquals(replyText, clientChannel.receiveText())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun handshakeWithWrongSecret_failsBothSides() {
        val serverKey = BtProtocol.deriveMasterKey("AAAA-BBBB-CCCC-DDDD")
        val clientKey = BtProtocol.deriveMasterKey("XXXX-YYYY-ZZZZ-2222")

        val c2sOut = PipedOutputStream()
        val c2sIn = PipedInputStream(c2sOut, 8192)
        val s2cOut = PipedOutputStream()
        val s2cIn = PipedInputStream(s2cOut, 8192)

        val pool = Executors.newFixedThreadPool(2)
        try {
            val serverFuture = pool.submit {
                BtProtocol.performServerHandshake(
                    DataInputStream(c2sIn),
                    DataOutputStream(s2cOut),
                    serverKey
                )
            }
            val clientFuture = pool.submit {
                BtProtocol.performClientHandshake(
                    DataInputStream(s2cIn),
                    DataOutputStream(c2sOut),
                    clientKey
                )
            }

            try {
                serverFuture.get()
                fail("Сервер должен отклонить клиента с неверным ключом")
            } catch (e: Exception) {
                assertTrue(e.cause is BtProtocol.AuthException)
            }

            try {
                clientFuture.get()
                fail("Клиент должен получить отказ авторизации")
            } catch (e: Exception) {
                assertTrue(e.cause is BtProtocol.AuthException)
            }
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun tamperedCiphertext_rejectedByGcm() {
        val (clientChannel, serverOutPipe, serverInKey) = createMockedClientChannelToBuffer()
        clientChannel.sendText("Секретный OTP 123456")

        val rawBytes = serverOutPipe.toByteArray()
        // Подменяем один байт шифротекста ближе к концу кадра
        rawBytes[rawBytes.size - 3] = (rawBytes[rawBytes.size - 3].toInt() xor 0xFF).toByte()

        val receiver = BtProtocol.SecureChannel(
            input = DataInputStream(ByteArrayInputStream(rawBytes)),
            output = DataOutputStream(ByteArrayOutputStream()),
            sendKey = serverInKey.first,
            recvKey = serverInKey.second,
            sendDir = byteArrayOf(0x42, 0x54, 0x02, 0x00),
            recvDir = byteArrayOf(0x42, 0x54, 0x01, 0x00)
        )

        try {
            receiver.receiveText()
            fail("Подмена байта шифротекста должна вызывать SecurityException")
        } catch (e: SecurityException) {
            assertFalse(e is BtProtocol.AuthException)
        }
    }

    @Test
    fun replayFrame_rejectedBySeqCounter() {
        val (clientChannel, capturedBytesOut, keys) = createMockedClientChannelToBuffer()
        clientChannel.sendText("Команда 1")
        val firstFrameBytes = capturedBytesOut.toByteArray()

        // Склеиваем один и тот же зашифрованный кадр дважды (replay)
        val replayedStream = ByteArray(firstFrameBytes.size * 2)
        System.arraycopy(firstFrameBytes, 0, replayedStream, 0, firstFrameBytes.size)
        System.arraycopy(firstFrameBytes, 0, replayedStream, firstFrameBytes.size, firstFrameBytes.size)

        val receiver = BtProtocol.SecureChannel(
            input = DataInputStream(ByteArrayInputStream(replayedStream)),
            output = DataOutputStream(ByteArrayOutputStream()),
            sendKey = keys.first,
            recvKey = keys.second,
            sendDir = byteArrayOf(0x42, 0x54, 0x02, 0x00),
            recvDir = byteArrayOf(0x42, 0x54, 0x01, 0x00)
        )

        assertEquals("Команда 1", receiver.receiveText())
        try {
            receiver.receiveText()
            fail("Повторный кадр с тем же seq должен быть отклонён")
        } catch (e: SecurityException) {
            assertFalse(e is BtProtocol.AuthException)
        }
    }

    @Test
    fun oversizedFrame_rejectedWithoutOom() {
        val oversizedHeader = ByteBuffer.allocate(4)
            .putInt(BtProtocol.MAX_FRAME_BYTES + 1)
            .array()
        try {
            BtProtocol.readFrame(DataInputStream(ByteArrayInputStream(oversizedHeader)))
            fail("Кадр больше MAX_FRAME_BYTES должен отклоняться")
        } catch (_: IOException) {
            // ожидаемо
        }

        val zeroHeader = ByteBuffer.allocate(4).putInt(0).array()
        try {
            BtProtocol.readFrame(DataInputStream(ByteArrayInputStream(zeroHeader)))
            fail("Пустой кадр должен отклоняться")
        } catch (_: IOException) {
            // ожидаемо
        }
    }

    @Test
    fun phoneValidation_acceptsNormalAndRejectsMmiOrInjection() {
        assertEquals("+905551234567", BtProtocol.sanitizeAndValidatePhone("+90 555 123-45-67"))
        assertEquals("05551234567", BtProtocol.sanitizeAndValidatePhone("0(555) 123 45 67"))
        assertEquals("112", BtProtocol.sanitizeAndValidatePhone("112"))

        assertNull(BtProtocol.sanitizeAndValidatePhone("**21*+905551234567#"))
        assertNull(BtProtocol.sanitizeAndValidatePhone("*100#"))
        assertNull(BtProtocol.sanitizeAndValidatePhone("abc12345"))
        assertNull(BtProtocol.sanitizeAndValidatePhone("12"))
        assertNull(BtProtocol.sanitizeAndValidatePhone("+1234567890123456"))

        assertTrue(BtProtocol.isStrictValidPhone("+905551234567"))
        assertFalse(BtProtocol.isStrictValidPhone("+90 555 123"))
        assertFalse(BtProtocol.isStrictValidPhone("**21*123#"))
    }

    @Test
    fun secretNormalization_isCaseAndDashInsensitive() {
        val secret = BtProtocol.generatePairingSecret()
        assertNotNull(secret)
        assertTrue(BtProtocol.isValidSecret(secret))

        val k1 = BtProtocol.deriveMasterKey(secret)
        val k2 = BtProtocol.deriveMasterKey(secret.lowercase().replace("-", " "))
        assertTrue(k1.contentEquals(k2))
        assertFalse(BtProtocol.isValidSecret("SHORT-123"))
    }

    private fun createMockedClientChannelToBuffer(): Triple<BtProtocol.SecureChannel, ByteArrayOutputStream, Pair<ByteArray, ByteArray>> {
        val master = BtProtocol.deriveMasterKey("ABCD-EFGH-JKLM-NPQR")
        val c2sKey = BtProtocol.hmacSha256(master, "K1".toByteArray())
        val s2cKey = BtProtocol.hmacSha256(master, "K2".toByteArray())
        val outBuf = ByteArrayOutputStream()
        val channel = BtProtocol.SecureChannel(
            input = DataInputStream(ByteArrayInputStream(ByteArray(0))),
            output = DataOutputStream(outBuf),
            sendKey = c2sKey,
            recvKey = s2cKey,
            sendDir = byteArrayOf(0x42, 0x54, 0x01, 0x00),
            recvDir = byteArrayOf(0x42, 0x54, 0x02, 0x00)
        )
        return Triple(channel, outBuf, Pair(s2cKey, c2sKey))
    }
}
