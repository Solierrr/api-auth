package com.solaria.auth.service.impl

import com.solaria.auth.entity.OneTimeToken
import com.solaria.auth.enums.OneTimeTokenType
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.OneTimeTokenRepository
import com.solaria.auth.service.TokenService
import com.solaria.auth.security.TokenHashing
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class TokenServiceImpl(private val tokenRepository: OneTimeTokenRepository) : TokenService {
    override fun create(user: UserAccount, value: String, type: OneTimeTokenType, expiresAt: Instant): OneTimeToken =
        tokenRepository.save(OneTimeToken(user = user, tokenHash = TokenHashing.sha256(value), type = type, expiresAt = expiresAt))

    override fun consume(value: String, type: OneTimeTokenType): OneTimeToken {
        val token = tokenRepository.findByTokenHashAndType(TokenHashing.sha256(value), type)
            ?: throw NoSuchElementException("Token not found")
        check(token.consumedAt == null && token.expiresAt.isAfter(Instant.now())) { "Token is not valid" }
        token.consumedAt = Instant.now()
        return tokenRepository.save(token)
    }
}
