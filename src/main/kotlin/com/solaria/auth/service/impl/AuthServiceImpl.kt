package com.solaria.auth.service.impl

import com.solaria.auth.config.JwtProperties
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.security.JwtService
import com.solaria.auth.service.AuthService
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.RefreshTokenService
import com.solaria.auth.service.SessionService
import com.solaria.auth.service.UserService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class AuthServiceImpl(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val refreshTokenService: RefreshTokenService,
    private val sessionService: SessionService,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties
) : AuthService {
    override fun login(email: String, password: String, ip: String?, userAgent: String?, device: String?): AuthSession {
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password))
        return createSession(userService.recordLogin(userService.findByEmail(email)), ip, userAgent, device)
    }

    override fun refresh(refreshToken: String, ip: String?, userAgent: String?, device: String?): AuthSession {
        val storedToken = refreshTokenService.validate(refreshToken)
        val user = requireNotNull(storedToken.user)
        refreshTokenService.revoke(storedToken)
        return createSession(user, ip, userAgent, device)
    }

    override fun logout(user: UserAccount) = logoutAll(user)

    override fun logoutAll(user: UserAccount) {
        refreshTokenService.revokeAllFor(user)
        sessionService.revokeAllFor(user)
    }

    private fun createSession(user: UserAccount, ip: String?, userAgent: String?, device: String?): AuthSession {
        val refreshTokenValue = jwtService.generateRefreshToken(user)
        val refreshToken = refreshTokenService.create(user, refreshTokenValue)
        sessionService.create(user, refreshToken, ip, userAgent, device)
        return AuthSession(
            accessToken = jwtService.generateAccessToken(user),
            refreshToken = refreshTokenValue,
            accessTokenExpiresAt = Instant.now().plus(jwtProperties.accessTokenTtl)
        )
    }
}
