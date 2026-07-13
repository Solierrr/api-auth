package com.solaria.auth.dto.auth.request

data class ResetPasswordRequest(

    val token: String,

    @field:StrongPassword
    val password: String
)