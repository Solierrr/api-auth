package com.solaria.auth

import com.solaria.auth.calendar.GoogleCalendarTokenCipher
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import javax.crypto.AEADBadTagException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class GoogleCalendarTokenCipherTests {
    private val key = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
    private val cipher = GoogleCalendarTokenCipher(key)

    @Test
    fun `encrypted token round trips with the same context`() {
        val encrypted = cipher.encrypt("ya29.access-token", "tech-1:access")

        assertFalse(encrypted.toString(Charsets.UTF_8).contains("ya29.access-token"))
        assertEquals("ya29.access-token", cipher.decrypt(encrypted, "tech-1:access"))
    }

    @Test
    fun `each encryption uses a fresh nonce`() {
        assertFalse(cipher.encrypt("token", "tech-1:access").contentEquals(cipher.encrypt("token", "tech-1:access")))
    }

    @Test
    fun `ciphertext bound to another technician or token kind is rejected`() {
        val encrypted = cipher.encrypt("token", "tech-1:access")

        assertFailsWith<AEADBadTagException> { cipher.decrypt(encrypted, "tech-2:access") }
        assertFailsWith<AEADBadTagException> { cipher.decrypt(encrypted, "tech-1:refresh") }
    }

    @Test
    fun `missing or invalid key answers service unavailable`() {
        listOf("", "not-base64", Base64.getEncoder().encodeToString(ByteArray(16))).forEach {
            val exception = assertFailsWith<ResponseStatusException> { GoogleCalendarTokenCipher(it).encrypt("token", "tech-1:access") }
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.statusCode)
        }
    }
}
