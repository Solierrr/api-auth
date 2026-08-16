package com.solaria.auth.integration.persistence

import com.solaria.auth.dto.persistence.request.InternalUserProvisionRequest
import com.solaria.auth.dto.persistence.response.InternalUserProvisionResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.UUID

// Lançada quando a chamada M2M a api-persistence falha (após tentativa de retry em 401)
class PersistenceIntegrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// Cliente do endpoint M2M POST /internal/users em api-persistence
@Component
class PersistenceUserClient(
    private val restClient: RestClient,
    private val tokenClient: PersistenceServiceTokenClient
) {
    // cria ou confirma o User correspondente a este authId
    fun provisionUser(authId: UUID): InternalUserProvisionResponse {
        return try {
            // primeira tentativa, com o token de serviço atualmente em cache
            call(authId, tokenClient.accessToken())
        } catch (firstFailure: RestClientResponseException) {
            // tenta novamente se a exception for de auht (token expirado/inválido)
            if (firstFailure.statusCode.value() != 401) {
                throw PersistenceIntegrationException(
                    "Falha ao provisionar usuário em api-persistence: HTTP ${firstFailure.statusCode.value()}",
                    firstFailure
                )
            }
            // invalida o token cacheado
            tokenClient.invalidate()
            try {
                call(authId, tokenClient.accessToken())
            } catch (retryFailure: RestClientResponseException) {
                // segunda falha consecutiva -> desiste e propaga, sem tentar de novo aqui
                throw PersistenceIntegrationException(
                    "Falha ao provisionar usuário em api-persistence após retry: HTTP ${retryFailure.statusCode.value()}",
                    retryFailure
                )
            }
        }
    }

    // faz a chamada HTTP autenticada a POST /internal/users
    private fun call(authId: UUID, accessToken: String): InternalUserProvisionResponse =
        restClient.post()
            .uri("/internal/users")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            .body(InternalUserProvisionRequest(authId = authId))
            .retrieve()
            .body(InternalUserProvisionResponse::class.java)
            ?: throw PersistenceIntegrationException("Resposta vazia ao provisionar usuário em api-persistence")
}
