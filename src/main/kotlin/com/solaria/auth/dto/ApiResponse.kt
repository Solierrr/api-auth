package com.solaria.auth.dto

data class ApiResponse<T>(

    val status: String,

    val message: String,

    val data: T?
)