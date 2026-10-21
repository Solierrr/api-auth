package com.solaria.auth.service

import com.solaria.auth.mobile.MobileFlowException
import com.solaria.auth.repository.FederatedIdentityRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.firebase.FirebaseTokenVerifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class FirebasePasswordMigrationService(
    private val verifier: FirebaseTokenVerifier,
    private val identities: FederatedIdentityRepository,
    private val accounts: UserAccountRepository,
    private val users: UserService,
    private val sessions: SessionService
) {
    fun migrate(userId: UUID, idToken: String, password: String) {
        if (password.length < 12 || password.toByteArray().size > 72) throw MobileFlowException("PASSWORD_POLICY")
        val proof = verifier.verify(idToken)
        val identity = identities.findByIssuerAndSubject(proof.issuer, proof.subject)
            ?: throw MobileFlowException("IDENTITY_PROOF_REQUIRED")
        if (!proof.emailVerified || proof.signInProvider != "password" || identity.user?.id != userId) {
            throw MobileFlowException("IDENTITY_PROOF_REQUIRED")
        }
        val found = accounts.findById(userId).orElseThrow()
        val user = accounts.findByPrimaryEmailForUpdate(found.primaryEmail) ?: throw AccountUnavailableException()
        if (user.localCredential != null || proof.email?.trim()?.lowercase() != user.primaryEmail.lowercase()) {
            throw MobileFlowException("MIGRATION_NOT_ALLOWED")
        }
        user.emailVerifiedAt = user.emailVerifiedAt ?: Instant.now()
        users.updatePassword(user, password)
        sessions.revokeAllFor(user)
    }
}
