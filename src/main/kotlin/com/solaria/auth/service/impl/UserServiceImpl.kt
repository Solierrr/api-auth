package com.solaria.auth.service.impl

import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.entity.LocalCredential
import com.solaria.auth.entity.OutboxEvent
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.repository.OutboxEventRepository
import com.solaria.auth.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class UserServiceImpl(
    private val userAccountRepository: UserAccountRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {
    override fun create(email: String, rawPassword: String): UserAccount {
        val normalizedEmail = email.trim().lowercase()
        require(normalizedEmail.isNotBlank()) { "Email must not be blank" }
        check(!userAccountRepository.existsByPrimaryEmail(normalizedEmail)) { "Email is already registered" }
        val user = UserAccount(primaryEmail = normalizedEmail)
        user.localCredential = LocalCredential(
            user = user,
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword))
        )
        val savedUser = userAccountRepository.save(user)
        val userId = requireNotNull(savedUser.id)
        outboxEventRepository.save(
            OutboxEvent(
                aggregateType = "AUTH_USER",
                aggregateId = userId,
                eventType = "USER_REGISTERED",
                payload = "{\"authUserId\":\"$userId\"}"
            )
        )
        return savedUser
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): UserAccount = userAccountRepository.findById(id).orElseThrow { NoSuchElementException("User account not found") }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): UserAccount = userAccountRepository.findByPrimaryEmail(email.trim().lowercase())
        ?: throw NoSuchElementException("User account not found")

    override fun updatePassword(user: UserAccount, rawPassword: String): UserAccount {
        val credential = user.localCredential ?: LocalCredential(user = user).also { user.localCredential = it }
        credential.passwordHash = requireNotNull(passwordEncoder.encode(rawPassword))
        credential.passwordChangedAt = Instant.now()
        credential.updatedAt = Instant.now()
        user.updatedAt = Instant.now()
        return userAccountRepository.save(user)
    }

    override fun recordLogin(user: UserAccount): UserAccount {
        val now = Instant.now()
        user.lastLoginAt = now
        user.updatedAt = now
        return userAccountRepository.save(user)
    }

    override fun deactivate(user: UserAccount) {
        user.status = AccountStatus.DISABLED
        user.updatedAt = Instant.now()
        userAccountRepository.save(user)
    }
}
