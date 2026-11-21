package com.exantt.btrouterclient

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecretStorage {

    private const val TAG = "BT_SECRET_STORE"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "btrouter_psk_wrap_key"
    private const val ENC_PREFIX = "enc_v1:"
    private const val GCM_IV_LEN = 12
    private const val GCM_TAG_BITS = 128

    @Synchronized
    fun saveSecret(prefs: SharedPreferences, prefKey: String, plaintext: String) {
        try {
            val secretKey = getOrCreateKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            val encoded = ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString(prefKey, encoded).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка шифрования ключа в KeyStore")
        }
    }

    @Synchronized
    fun loadSecret(prefs: SharedPreferences, prefKey: String): String? {
        val raw = prefs.getString(prefKey, null) ?: return null
        if (raw.isBlank()) return null

        if (!raw.startsWith(ENC_PREFIX)) {
            // Бесшовная миграция открытого ключа с версий 1.0.1–1.0.2 в KeyStore
            saveSecret(prefs, prefKey, raw)
            return raw
        }

        return try {
            val payload = Base64.decode(raw.substring(ENC_PREFIX.length), Base64.DEFAULT)
            if (payload.size <= GCM_IV_LEN) {
                Log.w(TAG, "Повреждённый формат зашифрованного ключа")
                return null
            }
            val iv = payload.copyOfRange(0, GCM_IV_LEN)
            val ciphertext = payload.copyOfRange(GCM_IV_LEN, payload.size)

            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return null

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка расшифровки ключа из KeyStore")
            null
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
