package com.solaria.auth.security.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun solierOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Solier/api-auth")
                .description("API de autenticação e identidade da Solaria.")
                .version("v1"),
        )
}
