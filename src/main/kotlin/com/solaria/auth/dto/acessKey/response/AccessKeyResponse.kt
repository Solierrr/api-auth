package com.solaria.auth.dto.acessKey.response

import java.time.LocalDateTime

data class AccessKeyResponse(

    val id: Long,

    val code: String,

    val companyId: Long,

    val position: String,

    val maxUses: Int,

    val usedCount: Int,

    val expiresAt: LocalDateTime,

    val active: Boolean,

    val createdAt: LocalDateTime
)