package com.solaria.auth.dto.acessKey.request

import jakarta.validation.constraints.Min

data class UpdateAccessKeyRequest(

    @field:Min(1)
    val maxUses: Int
)