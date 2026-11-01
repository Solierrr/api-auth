package com.solaria.auth.dto.auth.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class UpdateLocaleRequest(
    @field:NotBlank
    @field:Pattern(regexp = "pt-BR|en-US|es-ES")
    val locale: String
)
