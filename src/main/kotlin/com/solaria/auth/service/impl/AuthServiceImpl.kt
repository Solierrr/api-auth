package com.solaria.auth.service.impl

import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.service.AuthService
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.AuthSessionIssuer
import com.solaria.auth.service.AuthenticationAttemptService
import com.solaria.auth.service.AccountUnavailableException
import com.solaria.auth.service.RefreshTokenService
import com.solaria.auth.service.UserService
import org.springframework.security.core.AuthenticationException
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
    private val authSessionIssuer: AuthSessionIssuer,
    private val authenticationAttemptService: AuthenticationAttemptService
) : AuthService {
    override fun register(email: String, password: String): UserAccount = userService.create(email, password)

    override fun login(email: String, password: String, ip: String?, userAgent: String?, device: String?): AuthSession {
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password))
        } catch (exception: AuthenticationException) {
            authenticationAttemptService.recordFailure(email, ip, userAgent, "password")
            throw exception
        }
        val user = userService.recordLogin(userService.findByEmail(email))
        authenticationAttemptService.recordSuccess(user, ip, userAgent, "password")
        return createSession(user, ip, userAgent, device)
    }

    override fun refresh(refreshToken: String, ip: String?, userAgent: String?, device: String?): AuthSession {
        val rotatedToken = refreshTokenService.rotate(refreshToken)
        val session = requireNotNull(rotatedToken.entity.session)
        val user = requireNotNull(session.user)
        if (user.status != AccountStatus.ACTIVE || user.lockedUntil?.isAfter(Instant.now()) == true) {
            throw AccountUnavailableException()
        }
        return authSessionIssuer.resume(user, session, rotatedToken.value)
    }

    override fun logout(userId: java.util.UUID) {
        val user = userService.findById(userId)
        refreshTokenService.revokeAllFor(user)
        authenticationAttemptService.recordLogout(user)
    }

    private fun createSession(user: UserAccount, ip: String?, userAgent: String?, device: String?): AuthSession {
        return authSessionIssuer.issue(user, arrayOf("password"), ip, userAgent, device)
    }
}
