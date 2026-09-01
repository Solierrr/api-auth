package com.solaria.auth.integration.persistence

import com.solaria.auth.dto.persistence.request.InternalUserProvisionRequest
import com.solaria.auth.dto.persistence.response.InternalUserProvisionResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

// Lançada quando a chamada a api-persistence (POST /internal/users) falha
class PersistenceIntegrationException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

// Cliente do endpoint interno POST /internal/users em api-persistence.
// /internal/** é rota de rede interna sem auth
@Component
class PersistenceUserClient(
    private val restClient: RestClient
) {
    // cria ou confirma o User correspondente a este authId
    // (idempotência garantida por authId do lado de api-persistence)
    fun provisionUser(authId: UUID): InternalUserProvisionResponse {
        return try {
            restClient.post()
                .uri("/internal/users")
                .body(InternalUserProvisionRequest(authId = authId))
                .retrieve()
                .body(InternalUserProvisionResponse::class.java)
                ?: throw PersistenceIntegrationException(
                    "Resposta vazia ao provisionar usuário em api-persistence"
                )
        } catch (failure: RestClientResponseException) {
            throw PersistenceIntegrationException(
                "Falha ao provisionar usuário em api-persistence: HTTP ${failure.statusCode.value()}",
                failure
            )
        }
    }
}
