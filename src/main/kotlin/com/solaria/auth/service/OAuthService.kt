package com.solaria.auth.service

import com.solaria.auth.entity.FederatedIdentity
import com.solaria.auth.entity.UserAccount

interface OAuthService {
    fun linkFirebaseIdentity(
        user: UserAccount,
        issuer: String,
        subject: String,
        email: String?,
        emailVerified: Boolean
    ): FederatedIdentity

    fun findLinkedIdentity(issuer: String, subject: String): FederatedIdentity?
}
