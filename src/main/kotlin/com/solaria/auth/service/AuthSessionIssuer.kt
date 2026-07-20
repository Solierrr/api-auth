package com.solaria.auth.service

import com.solaria.auth.config.JwtProperties
import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.security.JwtService
import org.springframework.stereotype.Service
import java.time.Instant

interface AuthSessionIssuer {
    fun issue(
        user: UserAccount,
        authenticationMethods: Array<String>,
        ip: String?,
        userAgent: String?,
        device: String?
    ): AuthSession

    fun resume(user: UserAccount, session: Session, refreshToken: String): AuthSession
}

@Service
class DefaultAuthSessionIssuer(
    private val sessionService: SessionService,
    private val refreshTokenService: RefreshTokenService,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties
) : AuthSessionIssuer {
    override fun issue(
        user: UserAccount,
        authenticationMethods: Array<String>,
        ip: String?,
        userAgent: String?,
        device: String?
    ): AuthSession {
        val session = sessionService.create(user, authenticationMethods, ip, userAgent, device)
        val refreshToken = refreshTokenService.issue(session)
        return resume(user, session, refreshToken.value)
    }

    override fun resume(user: UserAccount, session: Session, refreshToken: String): AuthSession = AuthSession(
        accessToken = jwtService.generateAccessToken(user, requireNotNull(session.id), session.authenticationMethods),
        refreshToken = refreshToken,
        accessTokenExpiresAt = Instant.now().plus(jwtProperties.accessTokenTtl),
        userId = requireNotNull(user.id),
        email = user.primaryEmail
    )
}
