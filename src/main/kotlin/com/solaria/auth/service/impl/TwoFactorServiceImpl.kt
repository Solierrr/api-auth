package com.solaria.auth.service.impl

import com.solaria.auth.entity.TwoFactor
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.TotpAlgorithm
import com.solaria.auth.repository.TwoFactorRepository
import com.solaria.auth.service.TwoFactorService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TwoFactorServiceImpl(private val twoFactorRepository: TwoFactorRepository) : TwoFactorService {
    override fun configure(user: UserAccount, secret: String, algorithm: TotpAlgorithm, digits: Short, period: Short): TwoFactor {
        require(digits.toInt() in setOf(6, 8)) { "TOTP digits must be 6 or 8" }
        require(period > 0) { "TOTP period must be positive" }
        val factor = twoFactorRepository.findByUserId(requireNotNull(user.id)) ?: TwoFactor(user = user)
        factor.secret = secret
        factor.algorithm = algorithm
        factor.digits = digits
        factor.period = period
        factor.enabled = false
        return twoFactorRepository.save(factor)
    }

    override fun enable(user: UserAccount): TwoFactor = changeEnabled(user, true)

    override fun disable(user: UserAccount): TwoFactor = changeEnabled(user, false)

    @Transactional(readOnly = true)
    override fun findByUser(user: UserAccount): TwoFactor? = twoFactorRepository.findByUserId(requireNotNull(user.id))

    private fun changeEnabled(user: UserAccount, enabled: Boolean): TwoFactor {
        val factor = twoFactorRepository.findByUserId(requireNotNull(user.id)) ?: throw NoSuchElementException("Two-factor authentication is not configured")
        factor.enabled = enabled
        return twoFactorRepository.save(factor)
    }
}
