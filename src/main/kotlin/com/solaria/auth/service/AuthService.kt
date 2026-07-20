package com.solaria.auth.service

import com.solaria.auth.entity.UserAccount
import java.time.Instant
import java.util.UUID

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: Instant,
    val userId: UUID,
    val email: String
)

interface AuthService {
    fun register(email: String, password: String): UserAccount
    fun login(email: String, password: String, ip: String? = null, userAgent: String? = null, device: String? = null): AuthSession
    fun refresh(refreshToken: String, ip: String? = null, userAgent: String? = null, device: String? = null): AuthSession
    fun logout(userId: UUID)
}
