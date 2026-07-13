package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.Email

data class RegisterContractorRequest(

    val accessKey: String,

    val name: String,

    @field:ValidCpf
    val cpf: String,

    @field:Email
    val email: String,

    @field:StrongPassword
    val password: String
)