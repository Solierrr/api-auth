package com.solaria.auth.dto.acessKey.request

import jakarta.validation.constraints.Future
import java.time.LocalDateTime

data class RenewAccessKeyRequest(

    @field:Future
    val expiresAt: LocalDateTime
)