package com.solaria.auth.service.impl

import com.solaria.auth.entity.Token
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.TokenRepository
import com.solaria.auth.service.TokenService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class TokenServiceImpl(private val tokenRepository: TokenRepository) : TokenService {
    override fun create(user: UserAccount, value: String, type: String, expiresAt: Instant): Token =
        tokenRepository.save(Token(user = user, token = value, type = type, expiresAt = expiresAt))

    override fun consume(value: String, type: String): Token {
        val token = tokenRepository.findByToken(value) ?: throw NoSuchElementException("Token not found")
        check(token.type == type && !token.used && token.expiresAt.isAfter(Instant.now())) { "Token is not valid" }
        token.used = true
        return tokenRepository.save(token)
    }
}
