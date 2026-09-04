package com.solaria.auth.integration.core

import com.solaria.auth.dto.core.request.InternalUserProvisionRequest
import com.solaria.auth.dto.core.response.InternalUserProvisionResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

// Lançada quando a chamada a api-core (POST /internal/users) falha
class CoreIntegrationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

// Cliente do endpoint interno POST /internal/users em api-core.
// /internal/** é rota de rede interna sem auth
@Component
class CoreUserClient(
    private val restClient: RestClient
) {
    // cria ou confirma o User correspondente a este authId
    // (idempotência garantida por authId do lado de api-core)
    fun provisionUser(authId: UUID): InternalUserProvisionResponse {
        return try {
            restClient.post()
                .uri("/internal/users")
                .body(InternalUserProvisionRequest(authId = authId))
                .retrieve()
                .body(InternalUserProvisionResponse::class.java)
                ?: throw CoreIntegrationException(
                    "Resposta vazia ao provisionar usuário em api-core"
                )
        } catch (failure: RestClientResponseException) {
            throw CoreIntegrationException(
                "Falha ao provisionar usuário em api-core: HTTP ${failure.statusCode.value()}",
                failure
            )
        }
    }
}
