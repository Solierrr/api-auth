package com.solaria.auth.service.impl

import com.solaria.auth.entity.TotpFactor
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.TotpAlgorithm
import com.solaria.auth.repository.TotpFactorRepository
import com.solaria.auth.service.TwoFactorService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TwoFactorServiceImpl(private val totpFactorRepository: TotpFactorRepository) : TwoFactorService {
    override fun configure(
        user: UserAccount,
        secretCiphertext: ByteArray,
        secretNonce: ByteArray,
        encryptionKeyId: String,
        algorithm: TotpAlgorithm,
        digits: Short,
        periodSeconds: Short
    ): TotpFactor {
        require(digits.toInt() in setOf(6, 8)) { "TOTP digits must be 6 or 8" }
        require(periodSeconds.toInt() in 15..60) { "TOTP period must be between 15 and 60 seconds" }
        require(secretCiphertext.isNotEmpty()) { "Encrypted TOTP secret must not be empty" }
        require(secretNonce.isNotEmpty()) { "TOTP secret nonce must not be empty" }
        val factor = totpFactorRepository.findByUserId(requireNotNull(user.id)) ?: TotpFactor(user = user)
        factor.secretCiphertext = secretCiphertext
        factor.secretNonce = secretNonce
        factor.encryptionKeyId = encryptionKeyId
        factor.algorithm = algorithm
        factor.digits = digits
        factor.periodSeconds = periodSeconds
        factor.enabledAt = null
        return totpFactorRepository.save(factor)
    }

    override fun enable(user: UserAccount): TotpFactor = findRequired(user).also {
        it.enabledAt = java.time.Instant.now()
        totpFactorRepository.save(it)
    }

    override fun disable(user: UserAccount): TotpFactor = findRequired(user).also {
        it.enabledAt = null
        totpFactorRepository.save(it)
    }

    @Transactional(readOnly = true)
    override fun findByUser(user: UserAccount): TotpFactor? = totpFactorRepository.findByUserId(requireNotNull(user.id))

    private fun findRequired(user: UserAccount): TotpFactor =
        totpFactorRepository.findByUserId(requireNotNull(user.id))
            ?: throw NoSuchElementException("Two-factor authentication is not configured")
}
