package com.solaria.auth.integration.email

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.integration.email")
data class EmailClientProperties(

    // URL base do api-email; vazia desliga o envio
    val baseUrl: String = "",

    // chave interna enviada no cabeçalho X-API-Key; vazia desliga o envio
    val apiKey: String = "",

    val connectTimeout: Duration = Duration.ofSeconds(3),

    val readTimeout: Duration = Duration.ofSeconds(15)
)
