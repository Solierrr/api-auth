package com.solaria.auth.repository

import com.solaria.auth.entity.Session
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SessionRepository : JpaRepository<Session, UUID> {
    fun findAllByUserIdAndRevokedAtIsNull(userId: UUID): List<Session>
}
