package com.solaria.auth.dto.auth.response

import java.time.Instant
import java.util.UUID

data class BrowserAuthResponse(
    val accessToken: String,
    val accessTokenExpiresAt: Instant,
    val userId: UUID,
    val email: String,
    val sessionId: UUID?,
    val clientId: String,
    val context: String,
)
