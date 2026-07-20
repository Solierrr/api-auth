package com.solaria.auth.service.impl

import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.service.AuthService
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.AuthSessionIssuer
import com.solaria.auth.service.RefreshTokenService
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
    private val authSessionIssuer: AuthSessionIssuer
) : AuthService {
    override fun register(email: String, password: String): UserAccount = userService.create(email, password)

    override fun login(email: String, password: String, ip: String?, userAgent: String?, device: String?): AuthSession {
        authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password))
        return createSession(userService.recordLogin(userService.findByEmail(email)), ip, userAgent, device)
    }

    override fun refresh(refreshToken: String, ip: String?, userAgent: String?, device: String?): AuthSession {
        val rotatedToken = refreshTokenService.rotate(refreshToken)
        val session = requireNotNull(rotatedToken.entity.session)
        val user = requireNotNull(session.user)
        check(user.status == AccountStatus.ACTIVE) { "User account is not active" }
        check(user.lockedUntil == null || user.lockedUntil!!.isBefore(Instant.now())) { "User account is temporarily locked" }
        return authSessionIssuer.resume(user, session, rotatedToken.value)
    }

    override fun logout(userId: java.util.UUID) {
        refreshTokenService.revokeAllFor(userService.findById(userId))
    }

    private fun createSession(user: UserAccount, ip: String?, userAgent: String?, device: String?): AuthSession {
        return authSessionIssuer.issue(user, arrayOf("password"), ip, userAgent, device)
    }
}
