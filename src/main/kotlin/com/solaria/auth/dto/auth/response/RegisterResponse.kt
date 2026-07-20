package com.solaria.auth.dto.auth.response

import java.util.UUID

data class RegisterResponse(

    val id: UUID,

    val email: String,

    val message: String
)
