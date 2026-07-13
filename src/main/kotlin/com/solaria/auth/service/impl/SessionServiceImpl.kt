package com.solaria.auth.service.impl

import com.solaria.auth.entity.RefreshToken
import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.SessionRepository
import com.solaria.auth.service.SessionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SessionServiceImpl(private val sessionRepository: SessionRepository) : SessionService {
    override fun create(user: UserAccount, refreshToken: RefreshToken, ip: String?, userAgent: String?, device: String?): Session =
        sessionRepository.save(Session(user = user, refreshToken = refreshToken, ip = ip, userAgent = userAgent, device = device))

    override fun revokeAllFor(user: UserAccount) {
        sessionRepository.findAllByUserIdAndRevokedFalse(requireNotNull(user.id)).forEach { it.revoked = true }
    }
}
