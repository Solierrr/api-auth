package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(

    @field:NotBlank
    val refreshToken: String
)
