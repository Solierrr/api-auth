package com.solaria.auth.service

import com.solaria.auth.entity.UserAccount
import java.util.UUID

interface UserService {
    fun create(email: String, rawPassword: String): UserAccount
    fun findById(id: UUID): UserAccount
    fun findByEmail(email: String): UserAccount
    fun updatePassword(user: UserAccount, rawPassword: String): UserAccount
    fun recordLogin(user: UserAccount): UserAccount
    fun deactivate(user: UserAccount)
}
