package com.solaria.auth.repository

import com.solaria.auth.entity.TotpFactor
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TotpFactorRepository : JpaRepository<TotpFactor, UUID> {
    fun findByUserId(userId: UUID): TotpFactor?
}
