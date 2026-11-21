package com.exantt.btrouterserver

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import java.util.UUID
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object BtProtocol {

    class AuthException(message: String, cause: Throwable? = null) : SecurityException(message, cause)

    // Собственный UUID сервиса BTRouter вместо стандартного SPP (00001101-...)
    val APP_UUID: UUID = UUID.fromString("7d4e9a2b-6c14-4f83-9b52-3e8a1d7c4f90")

    const val MAX_FRAME_BYTES = 64 * 1024

    const val HANDSHAKE_CHALLENGE: Byte = 0x01
    const val HANDSHAKE_RESPONSE: Byte = 0x02
    const val HANDSHAKE_OK: Byte = 0x03
    const val HANDSHAKE_FAIL: Byte = 0x04

    private val DIR_C2S = byteArrayOf(0x42, 0x54, 0x01, 0x00)
    private val DIR_S2C = byteArrayOf(0x42, 0x54, 0x02, 0x00)

    private const val NONCE_SIZE = 16
    private const val HMAC_SIZE = 32
    private const val GCM_TAG_BITS = 128
    private const val GCM_NONCE_SIZE = 12
    private const val GCM_TAG_BYTES = 16

    val PHONE_REGEX = Regex("^\\+?[0-9]{3,15}$")
    private val PHONE_STRIP_REGEX = Regex("[\\s\\-()]")
    private const val SECRET_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private val secureRandom = SecureRandom()

    fun generatePairingSecret(): String {
        val sb = StringBuilder(19)
        for (i in 0 until 16) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-')
            }
            sb.append(SECRET_ALPHABET[secureRandom.nextInt(SECRET_ALPHABET.length)])
        }
        return sb.toString()
    }

    fun normalizeSecret(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            if (!ch.isWhitespace() && ch != '-') {
                sb.append(ch)
            }
        }
        return sb.toString().uppercase(Locale.US)
    }

    fun isValidSecret(raw: String): Boolean {
        return normalizeSecret(raw).length >= 12
    }

    fun deriveMasterKey(rawSecret: String): ByteArray {
        val normalized = normalizeSecret(rawSecret)
        require(normalized.length >= 12) { "Ключ сопряжения должен содержать не менее 12 символов" }
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(("BTRouter-PSK-v2:$normalized").toByteArray(Charsets.UTF_8))
    }

    fun sanitizeAndValidatePhone(raw: String): String? {
        val cleaned = raw.replace(PHONE_STRIP_REGEX, "")
        return if (PHONE_REGEX.matches(cleaned)) cleaned else null
    }

    fun isStrictValidPhone(number: String): Boolean {
        return PHONE_REGEX.matches(number)
    }

    @Throws(IOException::class)
    fun writeFrame(out: DataOutputStream, payload: ByteArray) {
        if (payload.isEmpty() || payload.size > MAX_FRAME_BYTES) {
            throw IOException("Недопустимый размер кадра: ${payload.size}")
        }
        out.writeInt(payload.size)
        out.write(payload)
        out.flush()
    }

    @Throws(IOException::class)
    fun readFrame(input: DataInputStream): ByteArray {
        val len = input.readInt()
        if (len <= 0 || len > MAX_FRAME_BYTES) {
            throw IOException("Недопустимая длина кадра: $len")
        }
        val buf = ByteArray(len)
        input.readFully(buf)
        return buf
    }

    fun hmacSha256(key: ByteArray, vararg parts: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        for (part in parts) {
            mac.update(part)
        }
        return mac.doFinal()
    }

    @Throws(IOException::class, AuthException::class)
    fun performServerHandshake(
        input: DataInputStream,
        output: DataOutputStream,
        masterKey: ByteArray
    ): SecureChannel {
        val serverNonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(serverNonce)

        val challengeFrame = ByteArray(1 + NONCE_SIZE)
        challengeFrame[0] = HANDSHAKE_CHALLENGE
        System.arraycopy(serverNonce, 0, challengeFrame, 1, NONCE_SIZE)
        writeFrame(output, challengeFrame)

        val responseFrame = readFrame(input)
        if (responseFrame.size != 1 + NONCE_SIZE + HMAC_SIZE || responseFrame[0] != HANDSHAKE_RESPONSE) {
            trySendFail(output)
            throw AuthException("Некорректный формат ответа рукопожатия")
        }

        val clientNonce = responseFrame.copyOfRange(1, 1 + NONCE_SIZE)
        val clientProof = responseFrame.copyOfRange(1 + NONCE_SIZE, responseFrame.size)

        val expectedClientProof = hmacSha256(
            masterKey,
            "BTRouter-v2-C2S".toByteArray(Charsets.UTF_8),
            serverNonce,
            clientNonce
        )
        if (!MessageDigest.isEqual(expectedClientProof, clientProof)) {
            trySendFail(output)
            throw AuthException("Неверный ключ сопряжения клиента")
        }

        val serverProof = hmacSha256(
            masterKey,
            "BTRouter-v2-S2C".toByteArray(Charsets.UTF_8),
            serverNonce,
            clientNonce
        )
        val okFrame = ByteArray(1 + HMAC_SIZE)
        okFrame[0] = HANDSHAKE_OK
        System.arraycopy(serverProof, 0, okFrame, 1, HMAC_SIZE)
        writeFrame(output, okFrame)

        val c2sKey = hmacSha256(masterKey, "KEY-C2S".toByteArray(Charsets.UTF_8), serverNonce, clientNonce)
        val s2cKey = hmacSha256(masterKey, "KEY-S2C".toByteArray(Charsets.UTF_8), serverNonce, clientNonce)

        return SecureChannel(
            input = input,
            output = output,
            sendKey = s2cKey,
            recvKey = c2sKey,
            sendDir = DIR_S2C,
            recvDir = DIR_C2S
        )
    }

    @Throws(IOException::class, AuthException::class)
    fun performClientHandshake(
        input: DataInputStream,
        output: DataOutputStream,
        masterKey: ByteArray
    ): SecureChannel {
        val challengeFrame = readFrame(input)
        if (challengeFrame.size != 1 + NONCE_SIZE || challengeFrame[0] != HANDSHAKE_CHALLENGE) {
            throw AuthException("Некорректный вызов от сервера")
        }
        val serverNonce = challengeFrame.copyOfRange(1, challengeFrame.size)

        val clientNonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(clientNonce)

        val clientProof = hmacSha256(
            masterKey,
            "BTRouter-v2-C2S".toByteArray(Charsets.UTF_8),
            serverNonce,
            clientNonce
        )

        val responseFrame = ByteArray(1 + NONCE_SIZE + HMAC_SIZE)
        responseFrame[0] = HANDSHAKE_RESPONSE
        System.arraycopy(clientNonce, 0, responseFrame, 1, NONCE_SIZE)
        System.arraycopy(clientProof, 0, responseFrame, 1 + NONCE_SIZE, HMAC_SIZE)
        writeFrame(output, responseFrame)

        val resultFrame = readFrame(input)
        if (resultFrame.isNotEmpty() && resultFrame[0] == HANDSHAKE_FAIL) {
            throw AuthException("Сервер отклонил ключ сопряжения")
        }
        if (resultFrame.size != 1 + HMAC_SIZE || resultFrame[0] != HANDSHAKE_OK) {
            throw AuthException("Некорректный ответ авторизации сервера")
        }

        val serverProof = resultFrame.copyOfRange(1, resultFrame.size)
        val expectedServerProof = hmacSha256(
            masterKey,
            "BTRouter-v2-S2C".toByteArray(Charsets.UTF_8),
            serverNonce,
            clientNonce
        )
        if (!MessageDigest.isEqual(expectedServerProof, serverProof)) {
            throw AuthException("Подлинность сервера не подтверждена")
        }

        val c2sKey = hmacSha256(masterKey, "KEY-C2S".toByteArray(Charsets.UTF_8), serverNonce, clientNonce)
        val s2cKey = hmacSha256(masterKey, "KEY-S2C".toByteArray(Charsets.UTF_8), serverNonce, clientNonce)

        return SecureChannel(
            input = input,
            output = output,
            sendKey = c2sKey,
            recvKey = s2cKey,
            sendDir = DIR_C2S,
            recvDir = DIR_S2C
        )
    }

    private fun trySendFail(output: DataOutputStream) {
        try {
            writeFrame(output, byteArrayOf(HANDSHAKE_FAIL))
        } catch (_: Exception) {
        }
    }

    class SecureChannel(
        private val input: DataInputStream,
        private val output: DataOutputStream,
        sendKey: ByteArray,
        recvKey: ByteArray,
        private val sendDir: ByteArray,
        private val recvDir: ByteArray
    ) {
        private val sendLock = Any()
        private val recvLock = Any()

        private var sendSeq = 1L
        private var expectedRecvSeq = 1L

        private val sendKeySpec = SecretKeySpec(sendKey.copyOf(), "AES")
        private val recvKeySpec = SecretKeySpec(recvKey.copyOf(), "AES")

        @Throws(IOException::class, SecurityException::class)
        fun sendEncrypted(plaintext: ByteArray) {
            synchronized(sendLock) {
                if (sendSeq == Long.MAX_VALUE) {
                    throw SecurityException("Исчерпан лимит счётчика сессии")
                }
                val seq = sendSeq
                val nonce = buildNonce(sendDir, seq)

                val ciphertext = try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, sendKeySpec, GCMParameterSpec(GCM_TAG_BITS, nonce))
                    cipher.updateAAD(nonce)
                    cipher.doFinal(plaintext)
                } catch (e: GeneralSecurityException) {
                    throw SecurityException("Ошибка шифрования кадра", e)
                }

                val frame = ByteBuffer.allocate(8 + ciphertext.size)
                    .putLong(seq)
                    .put(ciphertext)
                    .array()

                writeFrame(output, frame)
                sendSeq++
            }
        }

        @Throws(IOException::class, SecurityException::class)
        fun receiveEncrypted(): ByteArray {
            synchronized(recvLock) {
                val frame = readFrame(input)
                if (frame.size < 8 + GCM_TAG_BYTES) {
                    throw SecurityException("Слишком короткий зашифрованный кадр")
                }

                val seq = ByteBuffer.wrap(frame, 0, 8).long
                if (seq != expectedRecvSeq) {
                    throw SecurityException("Нарушение порядка или повтор кадра")
                }

                val nonce = buildNonce(recvDir, seq)
                val plaintext = try {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, recvKeySpec, GCMParameterSpec(GCM_TAG_BITS, nonce))
                    cipher.updateAAD(nonce)
                    cipher.doFinal(frame, 8, frame.size - 8)
                } catch (e: AEADBadTagException) {
                    throw SecurityException("Нарушена целостность зашифрованного кадра", e)
                } catch (e: GeneralSecurityException) {
                    throw SecurityException("Ошибка расшифровки кадра", e)
                }

                expectedRecvSeq++
                return plaintext
            }
        }

        @Throws(IOException::class, SecurityException::class)
        fun sendText(text: String) {
            sendEncrypted(text.toByteArray(Charsets.UTF_8))
        }

        @Throws(IOException::class, SecurityException::class)
        fun receiveText(): String {
            return String(receiveEncrypted(), Charsets.UTF_8)
        }

        private fun buildNonce(dir: ByteArray, seq: Long): ByteArray {
            val buf = ByteBuffer.allocate(GCM_NONCE_SIZE)
            buf.put(dir, 0, 4)
            buf.putLong(seq)
            return buf.array()
        }
    }
}
