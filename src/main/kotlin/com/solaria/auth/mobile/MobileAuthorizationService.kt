package com.solaria.auth.mobile

import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.repository.SessionRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.AuthUser
import com.solaria.auth.security.TokenHashing
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.AuthSessionIssuer
import com.solaria.auth.service.RefreshTokenService
import com.solaria.auth.service.SessionService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.util.UriComponentsBuilder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

data class StartMobile(val clientId: String, val redirectUri: String, val state: String, val codeChallenge: String, val codeChallengeMethod: String)
data class MobileGrant(val clientId: String, val redirectUri: String, val code: String, val codeVerifier: String)
data class StartedMobile(val transactionId: String, val authorizationUrl: String, val expiresAt: Instant)
data class MobileTransactionInfo(val transactionId: String, val clientId: String, val expiresAt: Instant, val email: String,
    val emailVerified: Boolean, val eligible: Boolean, val reasons: List<String>)

@Service
@Transactional
class MobileAuthorizationService(
    private val transactions: MobileTransactionRepository,
    private val users: UserAccountRepository,
    private val sessions: SessionRepository,
    private val sessionService: SessionService,
    private val refreshTokens: RefreshTokenService,
    private val issuer: AuthSessionIssuer,
    private val eligibility: MobileEligibilityService,
    @Value("\${app.mobile.client-id:solaria-mobile}") private val clientId: String,
    @Value("\${app.mobile.redirect-uris:}") private val redirectUris: String,
    @Value("\${app.mobile.web-authorize-url:}") private val webUrl: String,
    @Value("\${app.mobile.transaction-ttl-seconds:900}") private val transactionTtl: Long,
    @Value("\${app.mobile.code-ttl-seconds:60}") private val codeTtl: Long
) {
    fun start(request: StartMobile): StartedMobile {
        if (request.clientId != clientId || request.redirectUri !in redirectUris.split(',').map(String::trim)) {
            throw MobileFlowException("INVALID_CLIENT")
        }
        if (webUrl.isBlank() || transactionTtl !in 60..3600 || codeTtl !in 15..300) {
            throw MobileFlowException("MOBILE_AUTH_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE)
        }
        if (!request.state.matches(Regex("[A-Za-z0-9_-]{22,128}"))
            || request.codeChallengeMethod != "S256" || !request.codeChallenge.matches(Regex("[A-Za-z0-9_-]{43}"))) {
            throw MobileFlowException("INVALID_TRANSACTION")
        }
        val transaction = transactions.save(MobileTransaction(id = random(), clientId = clientId,
            redirectUri = request.redirectUri, state = request.state, codeChallenge = request.codeChallenge,
            expiresAt = Instant.now().plusSeconds(transactionTtl)))
        val url = UriComponentsBuilder.fromUriString(webUrl).queryParam("transaction", transaction.id).build().encode().toUriString()
        return StartedMobile(transaction.id, url, transaction.expiresAt)
    }

    fun inspect(id: String, principal: AuthUser): MobileTransactionInfo {
        val transaction = transaction(id)
        requireSession(principal)
        requireOwner(transaction, principal)
        val user = users.findById(principal.id).orElseThrow()
        val result = eligibility.evaluate(principal.id)
        return MobileTransactionInfo(transaction.id, transaction.clientId, transaction.expiresAt, user.primaryEmail,
            user.emailVerifiedAt != null, result.eligible && user.emailVerifiedAt != null, result.reasons)
    }

    fun authorize(id: String, principal: AuthUser, confirmed: Boolean): String {
        val transaction = transaction(id)
        requireOwner(transaction, principal)
        if (!confirmed) throw MobileFlowException("CONFIRMATION_REQUIRED")
        if (transaction.status != "PENDING_AUTHENTICATION") throw MobileFlowException("INVALID_TRANSACTION")
        val webSession = requireSession(principal)
        if (webSession.context != "web") throw MobileFlowException("WEB_SESSION_REQUIRED", HttpStatus.FORBIDDEN)
        val user = users.findById(principal.id).orElseThrow()
        if (user.emailVerifiedAt == null) throw MobileFlowException("EMAIL_NOT_VERIFIED", HttpStatus.FORBIDDEN)
        eligibility.requireEligible(principal.id)
        val code = random()
        transaction.userId = principal.id
        transaction.authorizingSessionId = principal.sessionId
        transaction.status = "AUTHORIZED"
        transaction.codeHash = TokenHashing.sha256(code)
        transaction.codeExpiresAt = Instant.now().plusSeconds(codeTtl).let { if (it.isBefore(transaction.expiresAt)) it else transaction.expiresAt }
        transactions.save(transaction)
        return UriComponentsBuilder.fromUriString(transaction.redirectUri).queryParam("code", code)
            .queryParam("state", transaction.state).build().encode().toUriString()
    }

    fun cancel(id: String, principal: AuthUser) {
        val transaction = transaction(id)
        requireSession(principal)
        requireOwner(transaction, principal)
        transaction.status = "CANCELLED"
        transactions.save(transaction)
    }

    fun redeem(request: MobileGrant, ip: String?, userAgent: String?): AuthSession {
        if (!request.code.matches(Regex("[A-Za-z0-9_-]{43}")) || !request.codeVerifier.matches(Regex("[A-Za-z0-9._~-]{43,128}"))) invalidGrant()
        val transaction = transactions.findByCodeHash(TokenHashing.sha256(request.code)) ?: invalidGrant()
        if (transaction.status != "AUTHORIZED" || transaction.consumedAt != null
            || !transaction.expiresAt.isAfter(Instant.now()) || transaction.codeExpiresAt?.isAfter(Instant.now()) != true
            || request.clientId != transaction.clientId || request.redirectUri != transaction.redirectUri
            || !pkceMatches(request.codeVerifier, transaction.codeChallenge)) invalidGrant()
        val webSession = sessions.findById(requireNotNull(transaction.authorizingSessionId)).orElse(null) ?: invalidGrant()
        if (webSession.revokedAt != null || !webSession.expiresAt.isAfter(Instant.now()) || webSession.user?.id != transaction.userId) invalidGrant()
        val user = users.findById(requireNotNull(transaction.userId)).orElse(null) ?: invalidGrant()
        if (user.status != AccountStatus.ACTIVE || user.emailVerifiedAt == null || user.lockedUntil?.isAfter(Instant.now()) == true) invalidGrant()
        eligibility.requireEligible(requireNotNull(user.id))
        val mobileSession = sessionService.create(user, webSession.authenticationMethods, ip, userAgent, "mobile")
        mobileSession.clientId = transaction.clientId
        mobileSession.context = "professional"
        sessions.save(mobileSession)
        val refresh = refreshTokens.issue(mobileSession)
        transaction.status = "CONSUMED"
        transaction.consumedAt = Instant.now()
        transactions.save(transaction)
        return issuer.resume(user, mobileSession, refresh.value)
    }

    private fun transaction(id: String): MobileTransaction {
        if (!id.matches(Regex("[A-Za-z0-9_-]{43}"))) throw MobileFlowException("INVALID_TRANSACTION")
        val transaction = transactions.lockById(id) ?: throw MobileFlowException("INVALID_TRANSACTION")
        if (!transaction.expiresAt.isAfter(Instant.now())) throw MobileFlowException("TRANSACTION_EXPIRED")
        if (transaction.status in listOf("CANCELLED", "CONSUMED")) throw MobileFlowException("INVALID_TRANSACTION")
        return transaction
    }
    private fun requireSession(principal: AuthUser): com.solaria.auth.entity.Session {
        val session = sessions.findById(principal.sessionId ?: invalidGrant()).orElse(null) ?: invalidGrant()
        if (session.revokedAt != null || !session.expiresAt.isAfter(Instant.now()) || session.user?.id != principal.id) invalidGrant()
        return session
    }
    private fun requireOwner(transaction: MobileTransaction, principal: AuthUser) {
        if (transaction.authorizingSessionId != null && transaction.authorizingSessionId != principal.sessionId) {
            throw MobileFlowException("INVALID_TRANSACTION")
        }
    }
    companion object {
        private val secureRandom = SecureRandom()
        fun random(): String = ByteArray(32).also(secureRandom::nextBytes).let(Base64.getUrlEncoder().withoutPadding()::encodeToString)
        fun pkceMatches(verifier: String, challenge: String): Boolean = MessageDigest.isEqual(
            Base64.getUrlEncoder().withoutPadding().encode(TokenHashing.sha256(verifier)), challenge.toByteArray(Charsets.US_ASCII))
        private fun invalidGrant(): Nothing = throw MobileFlowException("INVALID_GRANT")
    }
}
