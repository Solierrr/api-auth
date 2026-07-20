package com.solaria.auth.service.impl

import com.solaria.auth.entity.Profile
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.ProfileRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.service.UserService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class UserServiceImpl(
    private val userAccountRepository: UserAccountRepository,
    private val profileRepository: ProfileRepository,
    private val passwordEncoder: PasswordEncoder
) : UserService {
    override fun create(email: String, rawPassword: String): UserAccount {
        val normalizedEmail = email.trim().lowercase()
        require(normalizedEmail.isNotBlank()) { "Email must not be blank" }
        check(!userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) { "Email is already registered" }
        return userAccountRepository.save(UserAccount(email = normalizedEmail, passwordHash = passwordEncoder.encode(rawPassword)))
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): UserAccount = userAccountRepository.findById(id).orElseThrow { NoSuchElementException("User account not found") }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): UserAccount = userAccountRepository.findByEmailIgnoreCase(email.trim())
        ?: throw NoSuchElementException("User account not found")

    override fun createProfile(user: UserAccount, cpf: String, name: String, birthDate: LocalDate): Profile {
        check(user.profile == null) { "User already has a profile" }
        check(!profileRepository.existsByCpf(cpf)) { "CPF is already registered" }
        return profileRepository.save(Profile(user = user, cpf = cpf, name = name, birthDate = birthDate))
    }

    override fun updatePassword(user: UserAccount, rawPassword: String): UserAccount {
        user.passwordHash = passwordEncoder.encode(rawPassword)
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
        user.isActive = false
        user.updatedAt = Instant.now()
        userAccountRepository.save(user)
    }
}
