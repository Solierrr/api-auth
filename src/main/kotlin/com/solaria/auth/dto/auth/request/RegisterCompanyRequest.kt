package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.Email

data class RegisterCompanyRequest(

    @field:ValidCnpj
    val cnpj: String,

    val corporateName: String,

    val fantasyName: String,

    val companyEmail: String,

    val ownerName: String,

    @field:ValidCpf
    val ownerCpf: String,

    @field:Email
    val ownerEmail: String,

    @field:StrongPassword
    val password: String
)