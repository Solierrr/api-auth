package com.solaria.auth.integration.core

import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

// factory responsável por criar as chamadas http

@Configuration
// Liga CoreClientProperties como Bean
@EnableConfigurationProperties(CoreClientProperties::class)
class CoreClientConfig {

    // Monta o RestClient usado para chamar api-core, com timeouts explícitos e URL base fixa
    @Bean
    fun coreRestClient(
        properties: CoreClientProperties,
        observationRegistry: ObservationRegistry,
    ): RestClient {
        // Factory simples (HttpURLConnection, HTTP/1.1): o HttpClient do JDK negociava HTTP/2 com o edge do
        // Render e as chamadas terminavam em "Request cancelled", então o provisionamento nunca chegava na api-core
        val requestFactory = SimpleClientHttpRequestFactory()
        requestFactory.setConnectTimeout(properties.connectTimeout)
        requestFactory.setReadTimeout(properties.readTimeout)
        // client final, com a URL base de api-core já fixada
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .observationRegistry(observationRegistry)
            .apply { if (properties.serviceKey.isNotBlank()) it.defaultHeader("X-Service-Key", properties.serviceKey) }
            .build()
    }
}
