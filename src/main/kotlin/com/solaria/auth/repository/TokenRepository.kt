package com.solaria.auth.repository

import com.solaria.auth.entity.Token
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TokenRepository : JpaRepository<Token, UUID> {
    fun findByToken(token: String): Token?
}
