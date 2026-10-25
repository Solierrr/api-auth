package com.solaria.auth.service

import com.solaria.auth.entity.OneTimeToken
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.enums.OneTimeTokenType
import com.solaria.auth.mobile.MobileAuthorizationService
import com.solaria.auth.mobile.MobileFlowException
import com.solaria.auth.repository.OneTimeTokenRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.TokenHashing
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
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
    private val mail: ObjectProvider<JavaMailSender>,
    @Value("\${app.email.enabled:false}") private val enabled: Boolean,
    @Value("\${app.email.from:}") private val from: String,
    @Value("\${app.email.verification-url:}") private val verificationUrl: String,
    @Value("\${app.email.reset-url:}") private val resetUrl: String
) {
    fun requestVerification(userId: UUID) {
        checkTransport(verificationUrl)
        val found = users.findById(userId).orElseThrow()
        val user = users.findByPrimaryEmailForUpdate(found.primaryEmail) ?: throw AccountUnavailableException()
        if (user.emailVerifiedAt != null) return
        send(user, OneTimeTokenType.EMAIL_VERIFICATION, verificationUrl, "Confirme seu email no Solaria")
    }

    fun requestReset(email: String) {
        checkTransport(resetUrl)
        val user = users.findByPrimaryEmailForUpdate(email.trim().lowercase()) ?: return
        if (user.localCredential == null || user.status != AccountStatus.ACTIVE) return
        send(user, OneTimeTokenType.PASSWORD_RESET, resetUrl, "Redefina sua senha no Solaria")
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
        if (!passwords.matches(current, credential.passwordHash)) throw MobileFlowException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED)
        credential.passwordHash = requireNotNull(passwords.encode(password))
        credential.passwordChangedAt = Instant.now()
        credential.updatedAt = Instant.now()
        users.save(user)
        sessions.revokeAllFor(user)
    }

    private fun send(user: com.solaria.auth.entity.UserAccount, type: OneTimeTokenType, url: String, subject: String) {
        val previous = tokens.findFirstByUserIdAndTypeOrderByCreatedAtDesc(requireNotNull(user.id), type)
        if (previous?.createdAt?.isAfter(Instant.now().minusSeconds(60)) == true) return
        val token = MobileAuthorizationService.random()
        tokens.save(OneTimeToken(user = user, type = type, tokenHash = TokenHashing.sha256(token),
            expiresAt = Instant.now().plusSeconds(if (type == OneTimeTokenType.PASSWORD_RESET) 1800 else 86400)))
        val link = UriComponentsBuilder.fromUriString(url).queryParam("token", token).build().encode().toUriString()
        val message = SimpleMailMessage().apply {
            setFrom(from)
            setTo(user.primaryEmail)
            setSubject(subject)
            setText("Abra este link para continuar: $link\nSe não solicitou esta ação, ignore este email.")
        }
        try { requireNotNull(mail.ifAvailable).send(message) }
        catch (_: Exception) { throw MobileFlowException("EMAIL_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE) }
    }

    private fun consume(value: String, type: OneTimeTokenType): OneTimeToken {
        if (!value.matches(Regex("[A-Za-z0-9_-]{43}"))) invalidToken()
        val token = tokens.findByTokenHashAndType(TokenHashing.sha256(value), type) ?: invalidToken()
        if (token.consumedAt != null || !token.expiresAt.isAfter(Instant.now()) || token.user?.status != AccountStatus.ACTIVE) invalidToken()
        token.consumedAt = Instant.now()
        return tokens.save(token)
    }

    private fun checkTransport(url: String) {
        if (!enabled || from.isBlank() || url.isBlank() || mail.ifAvailable == null) {
            throw MobileFlowException("EMAIL_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE)
        }
    }

    private fun validatePassword(password: String) {
        if (password.length < 12 || password.toByteArray().size > 72) throw MobileFlowException("PASSWORD_POLICY")
    }

    private fun invalidToken(): Nothing = throw MobileFlowException("INVALID_OR_EXPIRED_TOKEN")
}
