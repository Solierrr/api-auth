package com.solaria.auth.calendar

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class GoogleCalendarTokenCipher(@Value("\${app.calendar.token-encryption-key:}") encodedKey: String) {
    private val key: SecretKeySpec? = runCatching { Base64.getDecoder().decode(encodedKey.trim()) }.getOrNull()
        ?.takeIf { it.size == KEY_BYTES }
        ?.let { SecretKeySpec(it, "AES") }
    private val random = SecureRandom()

    fun encrypt(plaintext: String, context: String): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = cipher(Cipher.ENCRYPT_MODE, nonce, context)
        return nonce + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(ciphertext: ByteArray, context: String): String {
        val cipher = cipher(Cipher.DECRYPT_MODE, ciphertext.copyOfRange(0, NONCE_BYTES), context)
        return String(cipher.doFinal(ciphertext, NONCE_BYTES, ciphertext.size - NONCE_BYTES), Charsets.UTF_8)
    }

    private fun cipher(mode: Int, nonce: ByteArray, context: String): Cipher {
        val secret = key ?: throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Calendar token encryption is not configured")
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, secret, GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(context.toByteArray(Charsets.UTF_8))
        }
    }

    private companion object {
        const val KEY_BYTES = 32
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
    }
}
