package com.solaria.auth.service

import com.solaria.auth.entity.OneTimeToken
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.enums.OneTimeTokenType
import com.solaria.auth.integration.email.EmailClient
import com.solaria.auth.integration.email.EmailDeliveryException
import com.solaria.auth.mobile.MobileAuthorizationService
import com.solaria.auth.mobile.MobileFlowException
import com.solaria.auth.repository.OneTimeTokenRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.TokenHashing
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class EmailCredentialService(
    private val users: UserAccountRepository,
    private val tokens: OneTimeTokenRepository,
    private val passwords: PasswordEncoder,
    private val sessions: SessionService,
    private val emailClient: EmailClient,
    @Value("\${app.email.verification-url:}") private val verificationUrl: String,
    @Value("\${app.email.reset-url:}") private val resetUrl: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun requestVerification(userId: UUID) {
        checkTransport(verificationUrl)
        val found = users.findById(userId).orElseThrow()
        val user = users.findByPrimaryEmailForUpdate(found.primaryEmail) ?: throw AccountUnavailableException()
        if (user.emailVerifiedAt != null) return
        send(user, OneTimeTokenType.EMAIL_VERIFICATION, verificationUrl, "email-verification")
    }

    fun requestReset(email: String) {
        checkTransport(resetUrl)
        val user = users.findByPrimaryEmailForUpdate(email.trim().lowercase()) ?: return
        if (user.localCredential == null || user.status != AccountStatus.ACTIVE) return
        send(user, OneTimeTokenType.PASSWORD_RESET, resetUrl, "password-reset")
    }

    fun confirmVerification(token: String) {
        val record = consume(token, OneTimeTokenType.EMAIL_VERIFICATION)
        val user = requireNotNull(record.user)
        user.emailVerifiedAt = Instant.now()
        users.save(user)
    }

    fun reset(token: String, password: String) {
        validatePassword(password)
        val record = consume(token, OneTimeTokenType.PASSWORD_RESET)
        val user = requireNotNull(record.user)
        val credential = user.localCredential ?: invalidToken()
        credential.passwordHash = requireNotNull(passwords.encode(password))
        credential.passwordChangedAt = Instant.now()
        credential.updatedAt = Instant.now()
        credential.mustChange = false
        users.save(user)
        sessions.revokeAllFor(user)
    }

    fun change(userId: UUID, current: String, password: String) {
        validatePassword(password)
        val found = users.findById(userId).orElseThrow()
        val user = users.findByPrimaryEmailForUpdate(found.primaryEmail) ?: throw AccountUnavailableException()
        val credential = user.localCredential ?: throw MobileFlowException("LOCAL_CREDENTIAL_REQUIRED")
        if (!passwords.matches(current, credential.passwordHash)) throw MobileFlowException("INVALID_CREDENTIAL", HttpStatus.UNAUTHORIZED)
        credential.passwordHash = requireNotNull(passwords.encode(password))
        credential.passwordChangedAt = Instant.now()
        credential.updatedAt = Instant.now()
        users.save(user)
        sessions.revokeAllFor(user)
    }

    private fun send(user: com.solaria.auth.entity.UserAccount, type: OneTimeTokenType, url: String, template: String) {
        val previous = tokens.findFirstByUserIdAndTypeOrderByCreatedAtDesc(requireNotNull(user.id), type)
        if (previous?.createdAt?.isAfter(Instant.now().minusSeconds(60)) == true) return
        val token = MobileAuthorizationService.random()
        tokens.save(OneTimeToken(user = user, type = type, tokenHash = TokenHashing.sha256(token),
            expiresAt = Instant.now().plusSeconds(if (type == OneTimeTokenType.PASSWORD_RESET) 1800 else 86400)))
        val link = UriComponentsBuilder.fromUriString(url).queryParam("token", token).build().encode().toUriString()
        try { emailClient.send(template, user.primaryEmail, link) }
        catch (failure: EmailDeliveryException) {
            log.warn("E-mail {} não enviado: {}", template, failure.message)
            throw MobileFlowException("EMAIL_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    private fun consume(value: String, type: OneTimeTokenType): OneTimeToken {
        if (!value.matches(Regex("[A-Za-z0-9_-]{43}"))) invalidToken()
        val token = tokens.findByTokenHashAndType(TokenHashing.sha256(value), type) ?: invalidToken()
        if (token.consumedAt != null || !token.expiresAt.isAfter(Instant.now()) || token.user?.status != AccountStatus.ACTIVE) invalidToken()
        token.consumedAt = Instant.now()
        return tokens.save(token)
    }

    private fun checkTransport(url: String) {
        if (!emailClient.configured || url.isBlank()) {
            throw MobileFlowException("EMAIL_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 12 || password.toByteArray().size > 72) throw MobileFlowException("PASSWORD_POLICY")
    }

    private fun invalidToken(): Nothing = throw MobileFlowException("INVALID_OR_EXPIRED_TOKEN")
}
