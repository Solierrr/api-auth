package com.solaria.auth.dto.acessKey.response

import java.time.LocalDateTime

data class AccessKeyValidationResponse(

    val valid: Boolean,

    val companyName: String?,

    val position: String?,

    val expiresAt: LocalDateTime?
)