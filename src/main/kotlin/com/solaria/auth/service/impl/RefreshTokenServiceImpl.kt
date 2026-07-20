package com.solaria.auth.service.impl

import com.solaria.auth.config.JwtProperties
import com.solaria.auth.entity.RefreshToken
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.RefreshTokenRepository
import com.solaria.auth.service.RefreshTokenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class RefreshTokenServiceImpl(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties
) : RefreshTokenService {
    override fun create(user: UserAccount, value: String): RefreshToken = refreshTokenRepository.save(
        RefreshToken(user = user, token = value, expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl))
    )

    override fun validate(value: String): RefreshToken {
        val token = refreshTokenRepository.findByToken(value) ?: throw NoSuchElementException("Refresh token not found")
        check(!token.revoked && token.expiresAt.isAfter(Instant.now())) { "Refresh token is not valid" }
        return token
    }

    override fun revoke(refreshToken: RefreshToken) {
        refreshToken.revoked = true
        refreshTokenRepository.save(refreshToken)
    }

    override fun revokeAllFor(user: UserAccount) {
        refreshTokenRepository.findAllByUserIdAndRevokedFalse(requireNotNull(user.id)).forEach { it.revoked = true }
    }
}
