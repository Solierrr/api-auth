package com.solaria.auth.integration.core

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

// Liga esta classe ao prefixo "app.integration.core" do application.properties
@ConfigurationProperties(prefix = "app.integration.core")
data class CoreClientProperties(

    // URL de api-core
    val baseUrl: String,

    // timeout de conexão da chamada HTTP a api-core
    val connectTimeout: Duration,

    // timeout de leitura da chamada HTTP a api-core
    val readTimeout: Duration
)
