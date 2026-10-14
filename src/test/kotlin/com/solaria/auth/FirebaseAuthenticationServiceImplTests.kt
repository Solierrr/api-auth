package com.solaria.auth

import com.solaria.auth.integration.core.CoreUserProvisioner
import com.solaria.auth.security.firebase.FirebaseTokenVerifier
import com.solaria.auth.security.firebase.VerifiedFirebaseToken
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.FirebaseAccountLinkMismatchException
import com.solaria.auth.service.AuthenticationAttemptService
import com.solaria.auth.service.VerifiedFirebaseEmailRequiredException
import com.solaria.auth.service.impl.FederatedLoginTransaction
import com.solaria.auth.service.impl.FirebaseAuthenticationServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FirebaseAuthenticationServiceImplTests {
    private lateinit var tokenVerifier: FirebaseTokenVerifier
    private lateinit var transaction: FederatedLoginTransaction
    private lateinit var service: FirebaseAuthenticationServiceImpl
    private lateinit var authenticationAttemptService: AuthenticationAttemptService
    private lateinit var coreUserProvisioner: CoreUserProvisioner

    @BeforeEach
    fun setUp() {
        tokenVerifier = Mockito.mock(FirebaseTokenVerifier::class.java)
        transaction = Mockito.mock(FederatedLoginTransaction::class.java)
        authenticationAttemptService = Mockito.mock(AuthenticationAttemptService::class.java)
        coreUserProvisioner = Mockito.mock(CoreUserProvisioner::class.java)
        service = FirebaseAuthenticationServiceImpl(tokenVerifier, transaction, authenticationAttemptService, coreUserProvisioner)
    }

    @Test
    fun `new firebase account is provisioned in core`() {
        val token = verifiedToken("user@example.com")
        val session = authSession(newlyRegistered = true)
        Mockito.`when`(tokenVerifier.verify("firebase-token")).thenReturn(token)
        Mockito.`when`(transaction.login(token, null, null, null)).thenReturn(session)

        val result = service.login("firebase-token")

        assertEquals(session, result)
        Mockito.verify(coreUserProvisioner).provision(session.userId)
    }

    @Test
    fun `existing firebase account is not provisioned again`() {
        val token = verifiedToken("user@example.com")
        val session = authSession(newlyRegistered = false)
        Mockito.`when`(tokenVerifier.verify("firebase-token")).thenReturn(token)
        Mockito.`when`(transaction.login(token, null, null, null)).thenReturn(session)

        val result = service.login("firebase-token")

        assertEquals(session, result)
        Mockito.verifyNoInteractions(coreUserProvisioner)
    }

    @Test
    fun `firebase email must match the local account email`() {
        Mockito.`when`(tokenVerifier.verify("firebase-token")).thenReturn(verifiedToken("other@example.com"))

        assertFailsWith<FirebaseAccountLinkMismatchException> {
            service.link("user@example.com", "password", "firebase-token")
        }

        Mockito.verifyNoInteractions(transaction)
    }

    @Test
    fun `unverified firebase email cannot be linked`() {
        Mockito.`when`(tokenVerifier.verify("firebase-token")).thenReturn(
            verifiedToken("user@example.com", emailVerified = false)
        )

        assertFailsWith<VerifiedFirebaseEmailRequiredException> {
            service.link("user@example.com", "password", "firebase-token")
        }

        Mockito.verifyNoInteractions(transaction)
    }

    private fun verifiedToken(email: String, emailVerified: Boolean = true) = VerifiedFirebaseToken(
        issuer = "https://securetoken.google.com/project-id",
        subject = "firebase-uid",
        email = email,
        emailVerified = emailVerified
    )

    private fun authSession(newlyRegistered: Boolean) = AuthSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        accessTokenExpiresAt = Instant.now().plusSeconds(900),
        userId = UUID.randomUUID(),
        email = "user@example.com",
        newlyRegistered = newlyRegistered
    )
}
