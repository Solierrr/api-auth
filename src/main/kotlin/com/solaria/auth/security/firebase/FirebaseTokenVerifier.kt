package com.solaria.auth.security.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

data class VerifiedFirebaseToken(
    val issuer: String,
    val subject: String,
    val email: String?,
    val emailVerified: Boolean
)

interface FirebaseTokenVerifier {
    fun verify(idToken: String): VerifiedFirebaseToken
}

@Component
@ConditionalOnProperty(prefix = "app.security.firebase", name = ["enabled"], havingValue = "true")
class FirebaseAdminTokenVerifier(
    private val firebaseAuth: FirebaseAuth,
    private val properties: FirebaseProperties
) : FirebaseTokenVerifier {
    override fun verify(idToken: String): VerifiedFirebaseToken {
        val decoded = try {
            firebaseAuth.verifyIdToken(idToken, true)
        } catch (exception: FirebaseAuthException) {
            if (exception.authErrorCode == null) {
                throw FirebaseUnavailableException()
            }
            throw InvalidFirebaseTokenException()
        } catch (_: IllegalArgumentException) {
            throw InvalidFirebaseTokenException()
        }
        return VerifiedFirebaseToken(
            issuer = "https://securetoken.google.com/${properties.projectId}",
            subject = decoded.uid,
            email = decoded.email,
            emailVerified = decoded.isEmailVerified
        )
    }
}

@Component
@ConditionalOnProperty(
    prefix = "app.security.firebase",
    name = ["enabled"],
    havingValue = "false",
    matchIfMissing = true
)
class DisabledFirebaseTokenVerifier : FirebaseTokenVerifier {
    override fun verify(idToken: String): VerifiedFirebaseToken = throw FirebaseUnavailableException()
}

class InvalidFirebaseTokenException : RuntimeException("Firebase ID token is invalid")

class FirebaseUnavailableException : RuntimeException("Firebase authentication is not configured")
