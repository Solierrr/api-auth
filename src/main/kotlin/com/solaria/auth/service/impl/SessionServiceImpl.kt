package com.solaria.auth.service.impl

import com.solaria.auth.config.JwtProperties
import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.SessionRepository
import com.solaria.auth.service.SessionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class SessionServiceImpl(
    private val sessionRepository: SessionRepository,
    private val jwtProperties: JwtProperties
) : SessionService {
    override fun create(user: UserAccount, authenticationMethods: Array<String>, ip: String?, userAgent: String?, device: String?): Session =
        sessionRepository.save(
            Session(
                user = user,
                authenticationMethods = authenticationMethods,
                ipAddress = ip,
                userAgent = userAgent,
                device = device,
                expiresAt = Instant.now().plus(jwtProperties.refreshTokenTtl)
            )
        )

    override fun revoke(session: Session, reason: String) {
        session.revokedAt = Instant.now()
        session.revocationReason = reason
        sessionRepository.save(session)
    }

    override fun revokeAllFor(user: UserAccount) {
        val now = Instant.now()
        sessionRepository.findAllByUserIdAndRevokedAtIsNull(requireNotNull(user.id)).forEach {
            it.revokedAt = now
            it.revocationReason = "USER_LOGOUT"
        }
    }
}
