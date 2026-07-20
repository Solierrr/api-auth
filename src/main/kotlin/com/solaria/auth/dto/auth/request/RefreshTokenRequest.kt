package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RefreshTokenRequest(

    @field:NotBlank
    @field:Size(max = 128)
    val refreshToken: String
)
