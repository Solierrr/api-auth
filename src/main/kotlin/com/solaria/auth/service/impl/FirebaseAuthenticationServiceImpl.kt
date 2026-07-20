package com.solaria.auth.service.impl

import com.solaria.auth.entity.FederatedIdentity
import com.solaria.auth.entity.OutboxEvent
import com.solaria.auth.entity.SecurityEvent
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.enums.SecurityEventType
import com.solaria.auth.repository.FederatedIdentityRepository
import com.solaria.auth.repository.OutboxEventRepository
import com.solaria.auth.repository.SecurityEventRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.firebase.FirebaseTokenVerifier
import com.solaria.auth.security.firebase.VerifiedFirebaseToken
import com.solaria.auth.service.AccountLinkRequiredException
import com.solaria.auth.service.AccountUnavailableException
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.AuthSessionIssuer
import com.solaria.auth.service.AuthenticationAttemptService
import com.solaria.auth.service.FederatedIdentityConflictException
import com.solaria.auth.service.FirebaseAccountLinkMismatchException
import com.solaria.auth.service.FirebaseAuthenticationService
import com.solaria.auth.service.InvalidAccountLinkCredentialsException
import com.solaria.auth.service.VerifiedFirebaseEmailRequiredException
import org.springframework.dao.DataIntegrityViolationException
import org.hibernate.exception.ConstraintViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class FirebaseAuthenticationServiceImpl(
    private val tokenVerifier: FirebaseTokenVerifier,
    private val federatedLoginTransaction: FederatedLoginTransaction,
    private val authenticationAttemptService: AuthenticationAttemptService
) : FirebaseAuthenticationService {
    override fun login(idToken: String, ip: String?, userAgent: String?, device: String?): AuthSession {
        val verifiedToken = tokenVerifier.verify(idToken)
        return try {
            federatedLoginTransaction.login(verifiedToken, ip, userAgent, device)
        } catch (exception: DataIntegrityViolationException) {
            if (!exception.isExpectedFederatedRace()) throw exception
            federatedLoginTransaction.login(verifiedToken, ip, userAgent, device)
        }
    }

    override fun link(
        email: String,
        password: String,
        idToken: String,
        ip: String?,
        userAgent: String?,
        device: String?
    ): AuthSession {
        val verifiedToken = tokenVerifier.verify(idToken)
        val verifiedEmail = verifiedToken.verifiedEmail()
        val normalizedEmail = email.trim().lowercase()
        if (verifiedEmail != normalizedEmail) {
            throw FirebaseAccountLinkMismatchException()
        }
        return try {
            federatedLoginTransaction.link(verifiedToken, normalizedEmail, password, ip, userAgent, device)
        } catch (exception: InvalidAccountLinkCredentialsException) {
            authenticationAttemptService.recordFailure(normalizedEmail, ip, userAgent, "firebase_link")
            throw exception
        } catch (exception: DataIntegrityViolationException) {
            if (!exception.isExpectedFederatedRace()) throw exception
            federatedLoginTransaction.link(verifiedToken, normalizedEmail, password, ip, userAgent, device)
        }
    }

    private fun VerifiedFirebaseToken.verifiedEmail(): String = email?.trim()?.lowercase()
        ?.takeIf { it.isNotBlank() && emailVerified }
        ?: throw VerifiedFirebaseEmailRequiredException()

    private fun DataIntegrityViolationException.isExpectedFederatedRace(): Boolean {
        val constraint = generateSequence<Throwable>(this) { it.cause }
            .filterIsInstance<ConstraintViolationException>()
            .firstOrNull()
            ?.constraintName
        return constraint in EXPECTED_RACE_CONSTRAINTS
    }

    private companion object {
        val EXPECTED_RACE_CONSTRAINTS = setOf(
            "uq_auth_user_primary_email",
            "uq_federated_identity_subject",
            "uq_federated_identity_user_issuer"
        )
    }
}

@Service
class FederatedLoginTransaction(
    private val federatedIdentityRepository: FederatedIdentityRepository,
    private val userAccountRepository: UserAccountRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val securityEventRepository: SecurityEventRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authSessionIssuer: AuthSessionIssuer,
    private val authenticationAttemptService: AuthenticationAttemptService
) {
    @Transactional
    fun login(token: VerifiedFirebaseToken, ip: String?, userAgent: String?, device: String?): AuthSession {
        val existingIdentity = federatedIdentityRepository.findByIssuerAndSubject(token.issuer, token.subject)
        val user = existingIdentity?.let { requireNotNull(it.user) } ?: createUserAndIdentity(token)
        requireActive(user)

        val now = Instant.now()
        existingIdentity?.let {
            it.email = token.email
            it.emailVerified = token.emailVerified
            it.lastLoginAt = now
            federatedIdentityRepository.save(it)
        }
        user.lastLoginAt = now
        user.updatedAt = now
        userAccountRepository.save(user)
        authenticationAttemptService.recordSuccess(user, ip, userAgent, "firebase")
        return authSessionIssuer.issue(user, arrayOf("firebase"), ip, userAgent, device)
    }

    @Transactional
    fun link(
        token: VerifiedFirebaseToken,
        email: String,
        password: String,
        ip: String?,
        userAgent: String?,
        device: String?
    ): AuthSession {
        val user = userAccountRepository.findByPrimaryEmail(email) ?: throw InvalidAccountLinkCredentialsException()
        requireActive(user)
        val credential = user.localCredential ?: throw InvalidAccountLinkCredentialsException()
        if (!passwordEncoder.matches(password, credential.passwordHash)) {
            throw InvalidAccountLinkCredentialsException()
        }

        val userId = requireNotNull(user.id)
        val existingIdentity = federatedIdentityRepository.findByIssuerAndSubject(token.issuer, token.subject)
        if (existingIdentity != null && existingIdentity.user?.id != userId) {
            throw FederatedIdentityConflictException()
        }

        val identity = existingIdentity ?: createLinkedIdentity(token, user, userId, ip, userAgent)
        val now = Instant.now()
        identity.email = email
        identity.emailVerified = true
        identity.lastLoginAt = now
        federatedIdentityRepository.save(identity)
        user.lastLoginAt = now
        user.updatedAt = now
        userAccountRepository.save(user)
        authenticationAttemptService.recordSuccess(user, ip, userAgent, "firebase_link")
        return authSessionIssuer.issue(user, arrayOf("password", "firebase"), ip, userAgent, device)
    }

    private fun createLinkedIdentity(
        token: VerifiedFirebaseToken,
        user: UserAccount,
        userId: java.util.UUID,
        ip: String?,
        userAgent: String?
    ): FederatedIdentity {
        if (federatedIdentityRepository.findByUserIdAndIssuer(userId, token.issuer) != null) {
            throw FederatedIdentityConflictException()
        }
        val identity = federatedIdentityRepository.save(
            FederatedIdentity(
                user = user,
                issuer = token.issuer,
                subject = token.subject,
                email = token.email,
                emailVerified = true
            )
        )
        securityEventRepository.save(
            SecurityEvent(
                user = user,
                eventType = SecurityEventType.FEDERATED_IDENTITY_LINKED,
                ipAddress = ip,
                userAgent = userAgent,
                details = "{\"authority\":\"FIREBASE\"}"
            )
        )
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "AUTH_USER",
                aggregateId = userId,
                eventType = "FEDERATED_IDENTITY_LINKED",
                payload = "{\"authUserId\":\"$userId\",\"authority\":\"FIREBASE\"}"
            )
        )
        return identity
    }

    private fun createUserAndIdentity(token: VerifiedFirebaseToken): UserAccount {
        val email = token.email?.trim()?.lowercase()
            ?.takeIf { it.isNotBlank() && token.emailVerified }
            ?: throw VerifiedFirebaseEmailRequiredException()
        if (userAccountRepository.existsByPrimaryEmail(email)) {
            throw AccountLinkRequiredException(email)
        }

        val user = userAccountRepository.save(UserAccount(primaryEmail = email, emailVerifiedAt = Instant.now()))
        val userId = requireNotNull(user.id)
        federatedIdentityRepository.save(
            FederatedIdentity(
                user = user,
                issuer = token.issuer,
                subject = token.subject,
                email = email,
                emailVerified = true,
                lastLoginAt = Instant.now()
            )
        )
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "AUTH_USER",
                aggregateId = userId,
                eventType = "USER_REGISTERED",
                payload = "{\"authUserId\":\"$userId\"}"
            )
        )
        return user
    }

    private fun requireActive(user: UserAccount) {
        if (user.status != AccountStatus.ACTIVE || user.lockedUntil?.isAfter(Instant.now()) == true) {
            throw AccountUnavailableException()
        }
    }
}
