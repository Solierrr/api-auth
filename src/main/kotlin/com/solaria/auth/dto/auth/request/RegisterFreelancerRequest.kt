package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.Email

data class RegisterFreelancerRequest(

    val name: String,

    @field:ValidCpf
    val cpf: String,

    @field:Email
    val email: String,

    @field:StrongPassword
    val password: String
)