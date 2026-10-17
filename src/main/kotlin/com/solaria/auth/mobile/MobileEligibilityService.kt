package com.solaria.auth.mobile

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
data class MobileEligibility(
    val authId: UUID,
    val professional: Boolean,
    val active: Boolean,
    val registrationComplete: Boolean,
    val approvalStatus: String,
    val eligible: Boolean,
    val reasons: List<String> = emptyList()
)

class MobileFlowException(val code: String, val status: HttpStatus = HttpStatus.BAD_REQUEST) : RuntimeException(code)

@Service
class MobileEligibilityService(
    private val coreRestClient: RestClient,
    @Value("\${app.mobile.service-token:}") private val token: String
) {
    fun evaluate(userId: UUID): MobileEligibility {
        if (token.length < 32) throw MobileFlowException("ELIGIBILITY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE)
        return try {
            val result = coreRestClient.get().uri("/internal/users/{id}/mobile-eligibility", userId)
                .header("Authorization", "Bearer $token").retrieve().body(MobileEligibility::class.java)
                ?: throw IllegalStateException()
            if (result.authId != userId) throw IllegalStateException()
            result
        } catch (_: Exception) {
            throw MobileFlowException("ELIGIBILITY_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    fun requireEligible(userId: UUID) {
        val result = evaluate(userId)
        if (!result.eligible || !result.professional || !result.active || !result.registrationComplete || result.approvalStatus != "APPROVED") {
            throw MobileFlowException("MOBILE_ACCOUNT_NOT_ALLOWED", HttpStatus.FORBIDDEN)
        }
    }
}
