package com.solaria.auth

import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.mobile.MobileAuthorizationService
import com.solaria.auth.mobile.MobileEligibilityService
import com.solaria.auth.mobile.MobileFlowException
import com.solaria.auth.mobile.MobileGrant
import com.solaria.auth.mobile.MobileTransaction
import com.solaria.auth.mobile.MobileTransactionRepository
import com.solaria.auth.mobile.StartMobile
import com.solaria.auth.repository.SessionRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.TokenHashing
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.AuthSessionIssuer
import com.solaria.auth.service.RefreshTokenService
import com.solaria.auth.service.SessionService
import com.solaria.auth.service.IssuedRefreshToken
import com.solaria.auth.entity.RefreshToken
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MobileAuthorizationServiceTests {
    private val transactions = Mockito.mock(MobileTransactionRepository::class.java)
    private val users = Mockito.mock(UserAccountRepository::class.java)
    private val sessions = Mockito.mock(SessionRepository::class.java)
    private val sessionService = Mockito.mock(SessionService::class.java)
    private val refresh = Mockito.mock(RefreshTokenService::class.java)
    private val issuer = Mockito.mock(AuthSessionIssuer::class.java)
    private val eligibility = Mockito.mock(MobileEligibilityService::class.java)
    private val callback = "https://app.example.com/auth/callback"
    private val service = MobileAuthorizationService(transactions, users, sessions, sessionService, refresh,
        issuer, eligibility, "solaria-mobile", callback, "https://web.example.com/pt-BR/auth/mobile", 900, 60)
    private val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
    private val challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"

    @Test
    fun `pkce accepts the rfc vector and rejects another verifier`() {
        assertTrue(MobileAuthorizationService.pkceMatches(verifier, challenge))
        assertFalse(MobileAuthorizationService.pkceMatches("a".repeat(43), challenge))
    }

    @Test
    fun `unregistered callbacks and plain pkce never create a transaction`() {
        assertFailsWith<MobileFlowException> { service.start(StartMobile("solaria-mobile", "$callback/other", "s".repeat(32), challenge, "S256")) }
        assertFailsWith<MobileFlowException> { service.start(StartMobile("solaria-mobile", callback, "s".repeat(32), challenge, "plain")) }
        Mockito.verifyNoInteractions(transactions)
    }

    @Test
    fun `redeeming creates a separate professional session and consumes the code once`() {
        val code = "c".repeat(43)
        val user = UserAccount(id = UUID.randomUUID(), primaryEmail = "user@example.com", emailVerifiedAt = Instant.now())
        val web = Session(id = UUID.randomUUID(), user = user, expiresAt = Instant.now().plusSeconds(600), authenticationMethods = arrayOf("password"))
        val mobile = Session(id = UUID.randomUUID(), user = user, expiresAt = Instant.now().plusSeconds(600))
        val transaction = MobileTransaction(id = "t".repeat(43), clientId = "solaria-mobile", redirectUri = callback,
            state = "s".repeat(32), codeChallenge = challenge, status = "AUTHORIZED", expiresAt = Instant.now().plusSeconds(600),
            userId = user.id, authorizingSessionId = web.id, codeHash = TokenHashing.sha256(code), codeExpiresAt = Instant.now().plusSeconds(60))
        Mockito.`when`(transactions.findByCodeHash(TokenHashing.sha256(code))).thenReturn(transaction)
        Mockito.`when`(sessions.findById(requireNotNull(web.id))).thenReturn(Optional.of(web))
        Mockito.`when`(users.findById(requireNotNull(user.id))).thenReturn(Optional.of(user))
        Mockito.`when`(sessionService.create(user, web.authenticationMethods, null, null, "mobile")).thenReturn(mobile)
        Mockito.`when`(refresh.issue(mobile)).thenReturn(IssuedRefreshToken(RefreshToken(session = mobile), "refresh"))
        Mockito.`when`(issuer.resume(user, mobile, "refresh")).thenReturn(AuthSession("access", "refresh", Instant.now(), requireNotNull(user.id), user.primaryEmail))
        val grant = MobileGrant("solaria-mobile", callback, code, verifier)
        assertEquals("access", service.redeem(grant, null, null).accessToken)
        assertEquals("professional", mobile.context)
        assertEquals("solaria-mobile", mobile.clientId)
        assertEquals("web", web.context)
        assertNotEquals(web.id, mobile.id)
        assertNotNull(transaction.consumedAt)
        assertFailsWith<MobileFlowException> { service.redeem(grant, null, null) }
        Mockito.verify(sessionService, Mockito.times(1)).create(user, web.authenticationMethods, null, null, "mobile")
    }

    @Test
    fun `wrong verifier leaves the authorization unconsumed`() {
        val code = "c".repeat(43)
        val transaction = MobileTransaction(id = "t".repeat(43), clientId = "solaria-mobile", redirectUri = callback,
            state = "s".repeat(32), codeChallenge = challenge, status = "AUTHORIZED", expiresAt = Instant.now().plusSeconds(600),
            codeHash = TokenHashing.sha256(code), codeExpiresAt = Instant.now().plusSeconds(60))
        Mockito.`when`(transactions.findByCodeHash(TokenHashing.sha256(code))).thenReturn(transaction)
        assertFailsWith<MobileFlowException> { service.redeem(MobileGrant("solaria-mobile", callback, code, "a".repeat(43)), null, null) }
        assertNull(transaction.consumedAt)
        Mockito.verifyNoInteractions(sessionService, refresh, issuer)
    }
}
