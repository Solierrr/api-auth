package com.solaria.auth.repository

import com.solaria.auth.entity.UserAccount
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface UserAccountRepository : JpaRepository<UserAccount, UUID> {
    fun existsByPrimaryEmail(email: String): Boolean
    fun findByPrimaryEmail(email: String): UserAccount?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserAccount user where user.primaryEmail = :email")
    fun findByPrimaryEmailForUpdate(@Param("email") email: String): UserAccount?
}
