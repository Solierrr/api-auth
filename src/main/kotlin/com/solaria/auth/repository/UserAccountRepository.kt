package com.solaria.auth.repository

import com.solaria.auth.entity.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserAccountRepository : JpaRepository<UserAccount, UUID> {
    fun existsByPrimaryEmail(email: String): Boolean
    fun findByPrimaryEmail(email: String): UserAccount?
}
