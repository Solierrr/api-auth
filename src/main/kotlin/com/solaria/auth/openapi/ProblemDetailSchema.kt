package com.solaria.auth.openapi

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

// Só documenta o corpo de erro (application/problem+json); quem monta o corpo é o ProblemDetailFactory
@Schema(name = ProblemDetailSchema.NAME, description = "Corpo de todo erro HTTP, no contrato de erro da organização")
data class ProblemDetailSchema(
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Sempre about:blank", example = "about:blank")
    val type: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "O mesmo valor de errorCode", example = "INVALID_CREDENTIALS")
    val title: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Código HTTP", example = "401")
    val status: Int,
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Código estável do catálogo de erros, usado pelo front para traduzir a mensagem",
        example = "INVALID_CREDENTIALS",
    )
    val errorCode: String,
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        description = "Explicação para quem depura; o front não exibe",
        example = "Email or password is invalid",
    )
    val detail: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Caminho da requisição", example = "/auth/login")
    val instance: String,
    @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Momento do erro, em UTC", example = "2026-10-24T13:45:00Z")
    val timestamp: Instant,
    @field:Schema(description = "Id do trace, para cruzar com os logs; presente quando há trace", example = "4bf92f3577b34da6a3ce929d0e0e4736")
    val traceId: String?,
    @field:Schema(description = "Erros por campo; presente só em VALIDATION_FAILED")
    val errors: List<FieldError>?,
) {
    @Schema(name = "ProblemFieldError", description = "Erro de validação de um campo ou parâmetro")
    data class FieldError(
        @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Campo ou parâmetro inválido", example = "email")
        val field: String,
        @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Código do erro do campo", example = "VALIDATION_FAILED")
        val errorCode: String,
        @field:Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Explicação para quem depura", example = "must be a well-formed email address")
        val detail: String,
    )

    companion object {
        const val NAME = "ProblemDetail"
    }
}
