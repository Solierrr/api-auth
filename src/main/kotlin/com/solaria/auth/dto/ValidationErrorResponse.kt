package com.solaria.auth.dto

data class ValidationErrorResponse(

    val status: String,

    val message: String,

    val errors: List<String>?
)