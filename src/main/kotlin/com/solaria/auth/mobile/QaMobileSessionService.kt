package com.solaria.auth.mobile

import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.repository.TotpFactorRepository
import com.solaria.auth.service.AuthenticationAttemptService
import com.solaria.auth.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant

@Service
@Transactional
class QaMobileSessionService(
    private val users: UserAccountRepository,
    private val factors: TotpFactorRepository,
    private val eligibility: MobileEligibilityService,
    private val mobile: MobileAuthorizationService,
    private val attempts: AuthenticationAttemptService,
    private val userService: UserService,
    private val environment: Environment,
    @Value("\${app.mobile.qa.enabled:false}") private val enabled: Boolean,
    @Value("\${app.mobile.qa.environment:local}") private val deployment: String,
    @Value("\${app.mobile.qa.account-email:}") private val accountEmail: String,
    @Value("\${app.mobile.qa.device-tokens:}") private val deviceTokens: String
) {
    fun login(deviceToken: String?, ip: String?, userAgent: String?): com.solaria.auth.service.AuthSession {
        if (!enabled || deployment != "qa" || environment.activeProfiles.any { it.equals("prod", true) || it.equals("production", true) }) {
            throw MobileFlowException("QA_LOGIN_DISABLED", HttpStatus.NOT_FOUND)
        }
        val allowed = deviceTokens.split(',').map(String::trim)
        if (accountEmail.isBlank() || !accountEmail.contains('@') || allowed.isEmpty() || allowed.any { !TOKEN.matches(it) }) {
            throw MobileFlowException("QA_LOGIN_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE)
        }
        if (deviceToken == null || !TOKEN.matches(deviceToken)) unauthorized()
        val supplied = digest(deviceToken)
        var accepted = false
        allowed.forEach { accepted = MessageDigest.isEqual(supplied, digest(it)) or accepted }
        if (!accepted) unauthorized()
        val user = users.findByPrimaryEmail(accountEmail.trim().lowercase()) ?: unavailable()
        val id = user.id ?: unavailable()
        if (user.status != AccountStatus.ACTIVE || user.emailVerifiedAt == null || user.lockedUntil?.isAfter(Instant.now()) == true) unavailable()
        if (factors.findByUserId(id)?.enabledAt != null) {
            throw MobileFlowException("QA_LOGIN_MFA_UNSUPPORTED", HttpStatus.FORBIDDEN)
        }
        eligibility.requireEligible(id)
        userService.recordLogin(user)
        attempts.recordSuccess(user, ip, userAgent, "qa_device")
        return mobile.issueSession(user, arrayOf("qa_device"), ip, userAgent)
    }

    private fun digest(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
    private fun unauthorized(): Nothing = throw MobileFlowException("QA_DEVICE_NOT_AUTHORIZED", HttpStatus.UNAUTHORIZED)
    private fun unavailable(): Nothing = throw MobileFlowException("MOBILE_ACCOUNT_NOT_ALLOWED", HttpStatus.FORBIDDEN)

    private companion object {
        val TOKEN = Regex("[A-Za-z0-9_-]{43,128}")
    }
}
