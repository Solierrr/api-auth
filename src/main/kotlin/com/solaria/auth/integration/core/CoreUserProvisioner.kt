package com.solaria.auth.integration.core

import com.solaria.auth.domain.entity.CoreProvisioningDlq
import com.solaria.auth.repository.CoreProvisioningDlqRepository
import org.slf4j.LoggerFactory
import org.springframework.core.retry.RetryException
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.UUID

// Cria o User em api-core logo depois que o cadastro no auth foi feito
// 1 chamada + 3 retries exponenciais
// Esgotadas as tentativas o authId vai para a DLQ (tabela core_provisioning_dlq) e o user é criado à mão
@Component
class CoreUserProvisioner(
    private val coreUserClient: CoreUserClient,
    private val dlqRepository: CoreProvisioningDlqRepository,
    initialDelay: Duration = Duration.ofSeconds(1)
) {
    private val log = LoggerFactory.getLogger(CoreUserProvisioner::class.java)

    private val retryTemplate = RetryTemplate(
        RetryPolicy.builder()
            .maxRetries(MAX_RETRIES)
            .delay(initialDelay)
            .multiplier(2.0)
            .build()
    )

    fun provision(authId: UUID) {
        var attempt = 0
        try {
            retryTemplate.execute {
                attempt++
                try {
                    coreUserClient.provisionUser(authId)
                } catch (failure: Exception) {
                    log.warn("Falha ao provisionar authId={} em api-core (tentativa {}/{}): {}", authId, attempt, MAX_RETRIES + 1, failure.message)
                    throw failure
                }
            }
        } catch (exhausted: RetryException) {
            deadLetter(authId, attempt, exhausted.lastException)
        }
    }

    private fun deadLetter(authId: UUID, attempts: Int, cause: Throwable?) {
        val error = cause?.message ?: cause?.javaClass?.simpleName ?: "unknown"
        log.error("CORE_PROVISIONING_DLQ authId={} attempts={} error={}", authId, attempts, error, cause)
        try {
            dlqRepository.save(CoreProvisioningDlq(authUserId = authId, lastError = error))
        } catch (saveFailure: Exception) {
            log.error("Falha ao gravar authId={} em core_provisioning_dlq; usar a linha CORE_PROVISIONING_DLQ acima", authId, saveFailure)
        }
    }

    private companion object {
        const val MAX_RETRIES = 3L
    }
}
