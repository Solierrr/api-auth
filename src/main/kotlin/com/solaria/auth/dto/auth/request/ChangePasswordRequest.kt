package com.solaria.auth.dto.auth.request

data class ChangePasswordRequest(

    val currentPassword: String,

    @field:StrongPassword
    val newPassword: String
)