package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.Email

data class ForgotPasswordRequest(

    @field:Email
    val email: String
)