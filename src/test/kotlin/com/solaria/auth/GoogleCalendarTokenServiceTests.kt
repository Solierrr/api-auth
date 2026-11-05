package com.solaria.auth

import com.solaria.auth.calendar.GoogleCalendarToken
import com.solaria.auth.calendar.GoogleCalendarTokenCipher
import com.solaria.auth.calendar.GoogleCalendarTokenRepository
import com.solaria.auth.calendar.GoogleCalendarTokenService
import com.solaria.auth.calendar.GoogleCalendarTokenStatus
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GoogleCalendarTokenServiceTests {
    private val repository = Mockito.mock(GoogleCalendarTokenRepository::class.java)
    private val cipher = GoogleCalendarTokenCipher(Base64.getEncoder().encodeToString(ByteArray(32)))
    private val service = GoogleCalendarTokenService(repository, cipher)

    private fun savedToken(): GoogleCalendarToken {
        val captor = ArgumentCaptor.forClass(GoogleCalendarToken::class.java)
        Mockito.verify(repository).save(captor.capture())
        return captor.value
    }

    @Test
    fun `saving stores encrypted tokens as connected and reading decrypts them`() {
        service.save("tech-1", "access", "refresh")
        val stored = savedToken()

        assertEquals("tech-1", stored.technicianId)
        assertEquals(GoogleCalendarTokenStatus.CONNECTED, stored.status)
        Mockito.`when`(repository.findByTechnicianId("tech-1")).thenReturn(stored)

        val tokens = service.connected("tech-1")
        assertEquals("access", tokens.accessToken)
        assertEquals("refresh", tokens.refreshToken)
        assertEquals(GoogleCalendarTokenStatus.CONNECTED, tokens.status)
    }

    @Test
    fun `disconnect and revoke clear the secrets`() {
        listOf(GoogleCalendarTokenStatus.DISCONNECTED, GoogleCalendarTokenStatus.REVOKED).forEach { status ->
            Mockito.reset(repository)
            val token = GoogleCalendarToken(technicianId = "tech-1", accessToken = byteArrayOf(1), refreshToken = byteArrayOf(2))
            Mockito.`when`(repository.findByTechnicianId("tech-1")).thenReturn(token)

            if (status == GoogleCalendarTokenStatus.DISCONNECTED) service.disconnect("tech-1") else service.revoke("tech-1")

            assertEquals(status, savedToken().status)
            assertNull(token.accessToken)
            assertNull(token.refreshToken)
        }
    }

    @Test
    fun `unknown or not connected technicians are not found`() {
        Mockito.`when`(repository.findByTechnicianId("tech-2"))
            .thenReturn(GoogleCalendarToken(technicianId = "tech-2", status = GoogleCalendarTokenStatus.REVOKED))

        listOf<() -> Unit>({ service.connected("tech-1") }, { service.connected("tech-2") },
            { service.disconnect("tech-1") }, { service.revoke("tech-1") }).forEach {
            assertEquals(HttpStatus.NOT_FOUND, assertFailsWith<ResponseStatusException> { it() }.statusCode)
        }
    }
}
