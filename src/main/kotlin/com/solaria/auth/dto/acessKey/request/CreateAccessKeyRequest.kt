package com.solaria.auth.dto.acessKey.request

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Min
import org.jetbrains.annotations.NotNull
import java.time.LocalDateTime

data class CreateAccessKeyRequest(

    @field:NotNull
    val positionId: Long,

    @field:Min(1)
    val maxUses: Int = 1,

    @field:Future
    val expiresAt: LocalDateTime
)