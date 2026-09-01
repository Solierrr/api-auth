package com.solaria.auth.integration.persistence

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

// Liga esta classe ao prefixo "app.integration.persistence" do application.properties
@ConfigurationProperties(prefix = "app.integration.persistence")
data class PersistenceClientProperties(

    // URL de api-persistence
    val baseUrl: String,

    // timeout de conexão da chamada HTTP a api-persistence
    val connectTimeout: Duration,

    // timeout de leitura da chamada HTTP a api-persistence
    val readTimeout: Duration
)
