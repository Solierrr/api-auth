package com.solaria.auth.service

interface FirebaseAuthenticationService {
    fun login(idToken: String, ip: String? = null, userAgent: String? = null, device: String? = null): AuthSession
    fun link(
        email: String,
        password: String,
        idToken: String,
        ip: String? = null,
        userAgent: String? = null,
        device: String? = null
    ): AuthSession
}

class AccountLinkRequiredException(val email: String) : RuntimeException("Account linking is required")

class VerifiedFirebaseEmailRequiredException : RuntimeException("A verified Firebase email is required")

class AccountUnavailableException : RuntimeException("User account is unavailable")

class FirebaseAccountLinkMismatchException : RuntimeException("Firebase email does not match the account being linked")

class InvalidAccountLinkCredentialsException : RuntimeException("Local account credentials are invalid")

class FederatedIdentityConflictException : RuntimeException("Firebase identity is already linked to another account")
