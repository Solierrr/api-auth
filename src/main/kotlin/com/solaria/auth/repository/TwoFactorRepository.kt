package com.solaria.auth.repository

import com.solaria.auth.entity.TwoFactor
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TwoFactorRepository : JpaRepository<TwoFactor, UUID> {
    fun findByUserId(userId: UUID): TwoFactor?
}
