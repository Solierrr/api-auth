package com.solaria.auth.dto.auth.response

data class UserProfileResponse(

    val id: Long,

    val companyId: Long?,

    val name: String,

    val email: String,

    val position: String
)