package com.solaria.auth

import com.solaria.auth.security.firebase.FirebaseTokenVerifier
import com.solaria.auth.security.firebase.VerifiedFirebaseToken
import com.solaria.auth.service.FirebaseAccountLinkMismatchException
import com.solaria.auth.service.AuthenticationAttemptService
import com.solaria.auth.service.VerifiedFirebaseEmailRequiredException
import com.solaria.auth.service.impl.FederatedLoginTransaction
import com.solaria.auth.service.impl.FirebaseAuthenticationServiceImpl
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import kotlin.test.assertFailsWith

class FirebaseAuthenticationServiceImplTests {
    private lateinit var tokenVerifier: FirebaseTokenVerifier
    private lateinit var transaction: FederatedLoginTransaction
    private lateinit var service: FirebaseAuthenticationServiceImpl
    private lateinit var authenticationAttemptService: AuthenticationAttemptService

    @BeforeEach
    fun setUp() {
        tokenVerifier = Mockito.mock(FirebaseTokenVerifier::class.java)
        transaction = Mockito.mock(FederatedLoginTransaction::class.java)
        authenticationAttemptService = Mockito.mock(AuthenticationAttemptService::class.java)
        service = FirebaseAuthenticationServiceImpl(tokenVerifier, transaction, authenticationAttemptService)
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
}
