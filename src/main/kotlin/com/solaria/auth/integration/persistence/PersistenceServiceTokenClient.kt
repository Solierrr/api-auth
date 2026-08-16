package com.solaria.auth.integration.persistence

import com.solaria.auth.dto.persistence.request.ServiceTokenMintRequest
import com.solaria.auth.dto.persistence.request.ServiceTokenRefreshRequest
import com.solaria.auth.dto.persistence.response.ServiceTokenResponse
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val REFRESH_SKEW_SECONDS = 30L

private data class CachedServiceToken(
    val accessToken: String,
    val refreshToken: String,
    val safeUntil: Instant
)

// Cliente responsável por obter/cachear/renovar o token de serviço M2M de api-persistence
// (POST /internal/service-tokens e /internal/service-tokens/refresh)
@Component
class PersistenceServiceTokenClient(
    private val restClient: RestClient,
    private val properties: PersistenceClientProperties
) {
    // guarda o token cacheado entre chamadas
    @Volatile
    private var cached: CachedServiceToken? = null

    // trava usada para não mintar/renovar o token duas vezes em paralelo
    private val lock = ReentrantLock()

    // devolve um access token válido, criando ou renovando quando necessário
    fun accessToken(): String {
        // devolve o token cacheado se ainda estiver dentro da margem de segurança de tempo
        cached?.let { token -> if (Instant.now().isBefore(token.safeUntil)) return token.accessToken }
        // sem token utilizável em cache -> entra na seção crítica para mintar/renovar
        return lock.withLock { refreshLocked() }
    }

    // invalida o token cacheado, forçando um novo mint/refresh na próxima chamada (usado após 401)
    fun invalidate() {
        // limpa o cache para forçar remint na próxima accessToken()
        cached = null
    }

    // decide entre renovar via refresh token ou mintar do zero
    private fun refreshLocked(): String {
        // outra thread pode ter criado/renovado o token-> verificação novamente
        cached?.let { token -> if (Instant.now().isBefore(token.safeUntil)) return token.accessToken }

        val existing = cached
        val response = try {
            if (existing != null) {
                // já existe um refresh token válido em cache -> tenta renová-lo primeiro
                refreshRemote(existing.refreshToken)
            } else {
                // nenhum token em cache ainda-> minta um par novo com o clientSecret
                mintRemote()
            }
        } catch (refreshFailure: RestClientResponseException) {
            // refresh token pode ter expirado/sido invalidado do lado de api-persistence -> cai para mint completo
            mintRemote()
        }

        // grava o novo par no cache, com a margem de segurança descontada da expiração real
        cached = CachedServiceToken(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            safeUntil = Instant.now().plusSeconds(response.expiresIn - REFRESH_SKEW_SECONDS)
        )
        return response.accessToken
    }

    // chama POST /internal/service-tokens com o clientSecret compartilhado -> novo refresh e accesses token
    private fun mintRemote(): ServiceTokenResponse =
        restClient.post()
            .uri("/internal/service-tokens")
            .body(ServiceTokenMintRequest(clientSecret = properties.clientSecret))
            .retrieve()
            .body(ServiceTokenResponse::class.java)
            ?: throw IllegalStateException("Resposta vazia ao mintar token de serviço em api-persistence")

    // chama POST /internal/service-tokens/refresh com o refresh token cacheado -> novo refresh e accesses token
    private fun refreshRemote(refreshToken: String): ServiceTokenResponse =
        restClient.post()
            .uri("/internal/service-tokens/refresh")
            .body(ServiceTokenRefreshRequest(refreshToken = refreshToken))
            .retrieve()
            .body(ServiceTokenResponse::class.java)
            ?: throw IllegalStateException("Resposta vazia ao renovar token de serviço em api-persistence")
}
