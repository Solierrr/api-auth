package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class FirebaseAccountLinkRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 320)
    val email: String,

    @field:NotBlank
    @field:Size(max = 72)
    val password: String,

    @field:NotBlank
    @field:Size(max = 10000)
    val idToken: String,

    @field:Size(max = 255)
    val device: String? = null
)
