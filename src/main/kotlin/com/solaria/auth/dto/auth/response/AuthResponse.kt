package com.solaria.auth.dto.auth.response

data class AuthResponse(

    val accessToken: String,

    val refreshToken: String,

    val expiresIn: Long,

    val user: UserProfileResponse
)