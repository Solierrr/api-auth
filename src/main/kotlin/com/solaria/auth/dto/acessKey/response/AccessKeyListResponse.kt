package com.solaria.auth.dto.acessKey.response

import java.time.LocalDateTime

data class AccessKeyListResponse(

    val id: Long,

    val code: String,

    val position: String,

    val active: Boolean,

    val usedCount: Int,

    val maxUses: Int,

    val expiresAt: LocalDateTime
)