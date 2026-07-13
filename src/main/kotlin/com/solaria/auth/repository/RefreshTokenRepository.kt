package com.solaria.auth.repository

import com.solaria.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
    fun findByToken(token: String): RefreshToken?
    fun findAllByUserIdAndRevokedFalse(userId: UUID): List<RefreshToken>
}
