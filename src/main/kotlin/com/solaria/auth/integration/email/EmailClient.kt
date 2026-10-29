package com.solaria.auth.integration.email

import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

// Lançada quando o api-email não aceita o e-mail (rede, timeout ou resposta diferente de 2xx)
class EmailDeliveryException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// Cliente de POST /emails no api-email. Sem URL ou chave configuradas, `configured` é falso e nada é enviado.
@Component
@EnableConfigurationProperties(EmailClientProperties::class)
class EmailClient(private val properties: EmailClientProperties) {
    private val log = LoggerFactory.getLogger(javaClass)

    val configured: Boolean = properties.baseUrl.isNotBlank() && properties.apiKey.isNotBlank()

    // Factory simples (HttpURLConnection, HTTP/1.1), como no cliente do api-core, por causa do edge do Render
    private val restClient: RestClient? = if (configured) {
        val requestFactory = SimpleClientHttpRequestFactory()
        requestFactory.setConnectTimeout(properties.connectTimeout)
        requestFactory.setReadTimeout(properties.readTimeout)
        RestClient.builder().baseUrl(properties.baseUrl.trimEnd('/')).requestFactory(requestFactory).build()
    } else {
        null
    }

    init {
        if (!configured) log.warn("api-email não configurado (EMAIL_SERVICE_URL/EMAIL_SERVICE_API_KEY): e-mails transacionais desligados")
    }

    fun send(template: String, to: String, actionUrl: String) {
        val client = restClient ?: throw EmailDeliveryException("api-email não configurado")
        try {
            client.post()
                .uri("/emails")
                .header("X-API-Key", properties.apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("template" to template, "locale" to "pt-BR", "to" to to, "data" to mapOf("actionUrl" to actionUrl)))
                .retrieve()
                .toBodilessEntity()
        } catch (failure: RestClientResponseException) {
            throw EmailDeliveryException("Falha ao enviar o e-mail $template pelo api-email: HTTP ${failure.statusCode.value()}", failure)
        } catch (failure: RestClientException) {
            throw EmailDeliveryException("Falha ao enviar o e-mail $template pelo api-email", failure)
        }
    }
}
