package com.solaria.auth.dto

data class ErrorResponse(

    val status: String,

    val message: String,

    val errors: List<String>?
)