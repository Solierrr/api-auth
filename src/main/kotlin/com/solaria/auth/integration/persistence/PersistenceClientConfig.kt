package com.solaria.auth.integration.persistence

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient

// factory responsável por criar as chamadas http

@Configuration
// Liga PersistenceClientProperties como Bean
@EnableConfigurationProperties(PersistenceClientProperties::class)
class PersistenceClientConfig {

    // Monta o RestClient usado para chamar api-persistence, com timeouts explícitos e URL base fixa
    @Bean
    fun persistenceRestClient(properties: PersistenceClientProperties): RestClient {
        // HttpClient JDK nativo, com o timeout de conexão vindo da config
        val httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.connectTimeout)
            .build()
        // factory HTTP do RestClient sobre esse HttpClient
        val requestFactory = JdkClientHttpRequestFactory(httpClient)
        // timeout de leitura vindo da config, aplicado à factory
        requestFactory.setReadTimeout(properties.readTimeout)
        // client final, com a URL base de api-persistence já fixada
        return RestClient.builder()
            .baseUrl(properties.baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
