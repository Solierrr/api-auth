package com.solaria.auth.service

import com.solaria.auth.entity.SecurityEvent
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.SecurityEventType
import com.solaria.auth.repository.SecurityEventRepository
import com.solaria.auth.repository.UserAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class AuthenticationAttemptService(
    private val userAccountRepository: UserAccountRepository,
    private val securityEventRepository: SecurityEventRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordFailure(email: String, ip: String?, userAgent: String?, method: String) {
        val normalizedEmail = email.trim().lowercase()
        val user = userAccountRepository.findByPrimaryEmailForUpdate(normalizedEmail)
        user?.let {
            if (it.lockedUntil?.isAfter(Instant.now()) != true) {
                it.failedLoginAttempts += 1
                if (it.failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
                    it.lockedUntil = Instant.now().plus(LOCK_DURATION)
                }
                it.updatedAt = Instant.now()
                userAccountRepository.save(it)
            }
        }
        securityEventRepository.save(
            SecurityEvent(
                user = user,
                eventType = SecurityEventType.LOGIN_FAILED,
                succeeded = false,
                ipAddress = ip,
                userAgent = userAgent,
                details = "{\"method\":\"$method\"}"
            )
        )
    }

    @Transactional
    fun recordSuccess(user: UserAccount, ip: String?, userAgent: String?, method: String) {
        user.failedLoginAttempts = 0
        user.lockedUntil = null
        user.updatedAt = Instant.now()
        userAccountRepository.save(user)
        securityEventRepository.save(
            SecurityEvent(
                user = user,
                eventType = SecurityEventType.LOGIN_SUCCEEDED,
                ipAddress = ip,
                userAgent = userAgent,
                details = "{\"method\":\"$method\"}"
            )
        )
    }

    @Transactional
    fun recordLogout(user: UserAccount) {
        securityEventRepository.save(
            SecurityEvent(
                user = user,
                eventType = SecurityEventType.LOGOUT
            )
        )
    }

    private companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        val LOCK_DURATION: Duration = Duration.ofMinutes(15)
    }
}
