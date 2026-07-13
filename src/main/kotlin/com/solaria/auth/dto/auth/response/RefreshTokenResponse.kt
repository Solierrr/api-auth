package com.solaria.auth.dto.auth.response

data class RefreshTokenResponse(

    val accessToken: String,

    val refreshToken: String,

    val expiresIn: Long
)