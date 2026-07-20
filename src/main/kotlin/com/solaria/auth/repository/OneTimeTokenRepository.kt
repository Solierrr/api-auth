package com.solaria.auth.repository

import com.solaria.auth.entity.OneTimeToken
import com.solaria.auth.enums.OneTimeTokenType
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OneTimeTokenRepository : JpaRepository<OneTimeToken, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByTokenHashAndType(tokenHash: ByteArray, type: OneTimeTokenType): OneTimeToken?
}
