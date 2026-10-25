package com.solaria.auth.calendar

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

data class GoogleCalendarTokens(val accessToken: String, val refreshToken: String, val status: GoogleCalendarTokenStatus)

@Service
@Transactional
class GoogleCalendarTokenService(
    private val tokens: GoogleCalendarTokenRepository,
    private val cipher: GoogleCalendarTokenCipher
) {
    @Transactional(readOnly = true)
    fun connected(technicianId: String): GoogleCalendarTokens {
        val token = tokens.findByTechnicianId(technicianId)
            ?.takeIf { it.status == GoogleCalendarTokenStatus.CONNECTED }
            ?: throw notFound()
        return GoogleCalendarTokens(
            cipher.decrypt(requireNotNull(token.accessToken), "$technicianId:access"),
            cipher.decrypt(requireNotNull(token.refreshToken), "$technicianId:refresh"),
            token.status
        )
    }

    fun save(technicianId: String, accessToken: String, refreshToken: String) {
        val token = tokens.findByTechnicianId(technicianId) ?: GoogleCalendarToken(technicianId = technicianId)
        token.accessToken = cipher.encrypt(accessToken, "$technicianId:access")
        token.refreshToken = cipher.encrypt(refreshToken, "$technicianId:refresh")
        token.status = GoogleCalendarTokenStatus.CONNECTED
        token.updatedAt = Instant.now()
        tokens.save(token)
    }

    fun disconnect(technicianId: String) = clear(technicianId, GoogleCalendarTokenStatus.DISCONNECTED)

    fun revoke(technicianId: String) = clear(technicianId, GoogleCalendarTokenStatus.REVOKED)

    private fun clear(technicianId: String, status: GoogleCalendarTokenStatus) {
        val token = tokens.findByTechnicianId(technicianId) ?: throw notFound()
        token.accessToken = null
        token.refreshToken = null
        token.status = status
        token.updatedAt = Instant.now()
        tokens.save(token)
    }

    private fun notFound() = ResponseStatusException(HttpStatus.NOT_FOUND, "Google Calendar token not found")
}
