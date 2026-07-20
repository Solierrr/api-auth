package com.solaria.auth.dto.auth.response

import java.time.Instant
import java.util.UUID

data class AuthResponse(

    val accessToken: String,

    val refreshToken: String,

    val accessTokenExpiresAt: Instant,

    val userId: UUID,

    val email: String
)
