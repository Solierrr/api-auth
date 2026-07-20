package com.solaria.auth

import com.solaria.auth.entity.FederatedIdentity
import com.solaria.auth.entity.OutboxEvent
import com.solaria.auth.entity.SecurityEvent
import com.solaria.auth.entity.Session
import com.solaria.auth.entity.LocalCredential
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.FederatedIdentityRepository
import com.solaria.auth.repository.OutboxEventRepository
import com.solaria.auth.repository.SecurityEventRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.firebase.VerifiedFirebaseToken
import com.solaria.auth.service.AccountLinkRequiredException
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.AuthSessionIssuer
import com.solaria.auth.service.AuthenticationAttemptService
import com.solaria.auth.service.FederatedIdentityConflictException
import com.solaria.auth.service.InvalidAccountLinkCredentialsException
import com.solaria.auth.service.VerifiedFirebaseEmailRequiredException
import com.solaria.auth.service.impl.FederatedLoginTransaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class FederatedLoginTransactionTests {
    private lateinit var identityRepository: FederatedIdentityRepository
    private lateinit var userRepository: UserAccountRepository
    private lateinit var outboxRepository: OutboxEventRepository
    private lateinit var securityEventRepository: SecurityEventRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var sessionIssuer: AuthSessionIssuer
    private lateinit var authenticationAttemptService: AuthenticationAttemptService
    private lateinit var transaction: FederatedLoginTransaction

    @BeforeEach
    fun setUp() {
        identityRepository = Mockito.mock(FederatedIdentityRepository::class.java)
        userRepository = Mockito.mock(UserAccountRepository::class.java)
        outboxRepository = Mockito.mock(OutboxEventRepository::class.java)
        securityEventRepository = Mockito.mock(SecurityEventRepository::class.java)
        passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
        sessionIssuer = FakeSessionIssuer()
        authenticationAttemptService = Mockito.mock(AuthenticationAttemptService::class.java)
        transaction = FederatedLoginTransaction(
            identityRepository,
            userRepository,
            outboxRepository,
            securityEventRepository,
            passwordEncoder,
            sessionIssuer,
            authenticationAttemptService
        )
    }

    @Test
    fun `known firebase identity authenticates the linked user`() {
        val user = UserAccount(id = UUID.randomUUID(), primaryEmail = "user@example.com")
        val identity = FederatedIdentity(user = user, issuer = ISSUER, subject = SUBJECT)
        val expected = authSession(user)
        Mockito.`when`(identityRepository.findByIssuerAndSubject(ISSUER, SUBJECT)).thenReturn(identity)
        (sessionIssuer as FakeSessionIssuer).result = expected

        val result = transaction.login(verifiedToken(), null, null, null)

        assertEquals(expected, result)
        assertNotNull(user.lastLoginAt)
        assertNotNull(identity.lastLoginAt)
        Mockito.verify(identityRepository).save(identity)
        Mockito.verify(userRepository).save(user)
    }

    @Test
    fun `existing email requires explicit account linking`() {
        Mockito.`when`(userRepository.existsByPrimaryEmail("user@example.com")).thenReturn(true)

        val error = assertFailsWith<AccountLinkRequiredException> {
            transaction.login(verifiedToken(), null, null, null)
        }

        assertEquals("user@example.com", error.email)
        Mockito.verify(identityRepository, Mockito.never()).save(any(FederatedIdentity::class.java))
    }

    @Test
    fun `first verified firebase login creates user identity and outbox event`() {
        val userId = UUID.randomUUID()
        val expected = AuthSession(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            accessTokenExpiresAt = Instant.now().plusSeconds(900),
            userId = userId,
            email = "user@example.com"
        )
        (sessionIssuer as FakeSessionIssuer).result = expected
        Mockito.`when`(userRepository.save(any(UserAccount::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as UserAccount).apply { id = userId }
        }

        val result = transaction.login(verifiedToken(), "127.0.0.1", "test-agent", "test-device")

        assertEquals(expected, result)
        val identityCaptor = ArgumentCaptor.forClass(FederatedIdentity::class.java)
        Mockito.verify(identityRepository).save(identityCaptor.capture())
        assertEquals(userId, identityCaptor.value.user?.id)
        assertEquals(SUBJECT, identityCaptor.value.subject)
        Mockito.verify(outboxRepository).save(any(OutboxEvent::class.java))
    }

    @Test
    fun `unverified firebase email cannot create an account`() {
        assertFailsWith<VerifiedFirebaseEmailRequiredException> {
            transaction.login(verifiedToken(emailVerified = false), null, null, null)
        }

        Mockito.verify(userRepository, Mockito.never()).save(any(UserAccount::class.java))
        Mockito.verify(outboxRepository, Mockito.never()).save(any(OutboxEvent::class.java))
    }

    @Test
    fun `verified firebase identity links to password account transactionally`() {
        val user = localUser()
        val expected = authSession(user)
        (sessionIssuer as FakeSessionIssuer).result = expected
        Mockito.`when`(userRepository.findByPrimaryEmail("user@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("correct-password", "password-hash")).thenReturn(true)
        Mockito.`when`(identityRepository.save(any(FederatedIdentity::class.java))).thenAnswer { it.arguments[0] }

        val result = transaction.link(
            verifiedToken(),
            "user@example.com",
            "correct-password",
            "127.0.0.1",
            "test-agent",
            "test-device"
        )

        assertEquals(expected, result)
        assertContentEquals(arrayOf("password", "firebase"), (sessionIssuer as FakeSessionIssuer).authenticationMethods)
        Mockito.verify(securityEventRepository).save(any(SecurityEvent::class.java))
        Mockito.verify(outboxRepository).save(any(OutboxEvent::class.java))
    }

    @Test
    fun `bad password cannot link firebase identity`() {
        val user = localUser()
        Mockito.`when`(userRepository.findByPrimaryEmail("user@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("wrong-password", "password-hash")).thenReturn(false)

        assertFailsWith<InvalidAccountLinkCredentialsException> {
            transaction.link(verifiedToken(), "user@example.com", "wrong-password", null, null, null)
        }

        Mockito.verify(identityRepository, Mockito.never()).save(any(FederatedIdentity::class.java))
        Mockito.verify(securityEventRepository, Mockito.never()).save(any(SecurityEvent::class.java))
        Mockito.verify(outboxRepository, Mockito.never()).save(any(OutboxEvent::class.java))
    }

    @Test
    fun `firebase identity owned by another account cannot be linked`() {
        val user = localUser()
        val owner = UserAccount(id = UUID.randomUUID(), primaryEmail = "owner@example.com")
        val identity = FederatedIdentity(user = owner, issuer = ISSUER, subject = SUBJECT)
        Mockito.`when`(userRepository.findByPrimaryEmail("user@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("correct-password", "password-hash")).thenReturn(true)
        Mockito.`when`(identityRepository.findByIssuerAndSubject(ISSUER, SUBJECT)).thenReturn(identity)

        assertFailsWith<FederatedIdentityConflictException> {
            transaction.link(verifiedToken(), "user@example.com", "correct-password", null, null, null)
        }

        Mockito.verify(securityEventRepository, Mockito.never()).save(any(SecurityEvent::class.java))
        Mockito.verify(outboxRepository, Mockito.never()).save(any(OutboxEvent::class.java))
    }

    @Test
    fun `retrying an existing account link does not duplicate events`() {
        val user = localUser()
        val identity = FederatedIdentity(user = user, issuer = ISSUER, subject = SUBJECT)
        (sessionIssuer as FakeSessionIssuer).result = authSession(user)
        Mockito.`when`(userRepository.findByPrimaryEmail("user@example.com")).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("correct-password", "password-hash")).thenReturn(true)
        Mockito.`when`(identityRepository.findByIssuerAndSubject(ISSUER, SUBJECT)).thenReturn(identity)

        transaction.link(verifiedToken(), "user@example.com", "correct-password", null, null, null)

        Mockito.verify(identityRepository).save(identity)
        Mockito.verify(securityEventRepository, Mockito.never()).save(any(SecurityEvent::class.java))
        Mockito.verify(outboxRepository, Mockito.never()).save(any(OutboxEvent::class.java))
    }

    private fun verifiedToken(emailVerified: Boolean = true) = VerifiedFirebaseToken(
        issuer = ISSUER,
        subject = SUBJECT,
        email = "user@example.com",
        emailVerified = emailVerified
    )

    private fun authSession(user: UserAccount) = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        accessTokenExpiresAt = Instant.now().plusSeconds(900),
        userId = requireNotNull(user.id),
        email = user.primaryEmail
    )

    private fun localUser() = UserAccount(id = UUID.randomUUID(), primaryEmail = "user@example.com").also {
        it.localCredential = LocalCredential(user = it, passwordHash = "password-hash")
    }

    private class FakeSessionIssuer : AuthSessionIssuer {
        lateinit var result: AuthSession
        lateinit var authenticationMethods: Array<String>

        override fun issue(
            user: UserAccount,
            authenticationMethods: Array<String>,
            ip: String?,
            userAgent: String?,
            device: String?
        ): AuthSession {
            this.authenticationMethods = authenticationMethods
            return result
        }

        override fun resume(user: UserAccount, session: Session, refreshToken: String): AuthSession = result
    }

    private companion object {
        const val ISSUER = "https://securetoken.google.com/project-id"
        const val SUBJECT = "firebase-uid"
    }
}
