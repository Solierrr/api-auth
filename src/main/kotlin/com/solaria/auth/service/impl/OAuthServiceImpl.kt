package com.solaria.auth.service.impl

import com.solaria.auth.entity.FederatedIdentity
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.FederatedIdentityRepository
import com.solaria.auth.service.OAuthService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OAuthServiceImpl(private val federatedIdentityRepository: FederatedIdentityRepository) : OAuthService {
    override fun linkFirebaseIdentity(
        user: UserAccount,
        issuer: String,
        subject: String,
        email: String?,
        emailVerified: Boolean
    ): FederatedIdentity {
        val existing = federatedIdentityRepository.findByIssuerAndSubject(issuer, subject)
        check(existing == null || existing.user?.id == user.id) { "Federated identity is already linked to another user" }
        return existing ?: federatedIdentityRepository.save(
            FederatedIdentity(
                user = user,
                issuer = issuer,
                subject = subject,
                email = email,
                emailVerified = emailVerified
            )
        )
    }

    @Transactional(readOnly = true)
    override fun findLinkedIdentity(issuer: String, subject: String): FederatedIdentity? =
        federatedIdentityRepository.findByIssuerAndSubject(issuer, subject)
}
