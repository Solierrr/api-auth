package com.solaria.auth.service.impl

import com.solaria.auth.config.JwtProperties
import com.solaria.auth.entity.RefreshToken
import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.RefreshTokenRepository
import com.solaria.auth.security.TokenHashing
import com.solaria.auth.service.IssuedRefreshToken
import com.solaria.auth.service.RefreshTokenService
import com.solaria.auth.service.RefreshTokenReuseException
import com.solaria.auth.service.SessionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.security.SecureRandom
import java.util.Base64

@Service
@Transactional
class RefreshTokenServiceImpl(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProperties: JwtProperties,
    private val sessionService: SessionService
) : RefreshTokenService {
    override fun issue(session: Session): IssuedRefreshToken {
        val value = generateToken()
        val entity = refreshTokenRepository.save(
            RefreshToken(
                session = session,
                tokenHash = TokenHashing.sha256(value),
                expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl)
            )
        )
        return IssuedRefreshToken(entity, value)
    }

    @Transactional(noRollbackFor = [RefreshTokenReuseException::class])
    override fun rotate(value: String): IssuedRefreshToken {
        val current = refreshTokenRepository.findByTokenHash(TokenHashing.sha256(value))
            ?: throw NoSuchElementException("Refresh token not found")
        val session = requireNotNull(current.session)
        val now = Instant.now()

        if (current.consumedAt != null) {
            sessionService.revoke(session, "REFRESH_TOKEN_REUSED")
            throw RefreshTokenReuseException()
        }

        check(current.revokedAt == null && current.expiresAt.isAfter(now)) { "Refresh token is not valid" }
        check(session.revokedAt == null && session.expiresAt.isAfter(now)) { "Session is not valid" }

        current.consumedAt = now
        val replacement = issue(session)
        current.replacedBy = replacement.entity
        refreshTokenRepository.save(current)
        return replacement
    }

    override fun revokeAllFor(user: UserAccount) {
        sessionService.revokeAllFor(user)
    }

    private fun generateToken(): String = ByteArray(TOKEN_SIZE_BYTES)
        .also(secureRandom::nextBytes)
        .let(Base64.getUrlEncoder().withoutPadding()::encodeToString)

    private companion object {
        const val TOKEN_SIZE_BYTES = 32
        val secureRandom = SecureRandom()
    }
}
