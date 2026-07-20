package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FirebaseLoginRequest(
    @field:NotBlank
    @field:Size(max = 10000)
    val idToken: String,
    @field:Size(max = 255)
    val device: String? = null
)
