package com.solaria.auth.security.config

import com.solaria.auth.openapi.ProblemDetailSchema
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
class OpenApiConfig {

    @Bean
    fun solariaOpenApi(build: BuildProperties): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Solaria — api-auth")
                .description(
                    "Serviço de login e identidade da Solaria: cria contas, autentica com e-mail e senha ou pelo " +
                        "Firebase, confirma o e-mail, recupera a senha, autoriza o app mobile e emite os tokens JWT " +
                        "(RS256) que os outros serviços validam pelas chaves públicas em /.well-known/jwks.json. " +
                        "As rotas de login, cadastro, renovação e JWKS são públicas; as demais usam o access token " +
                        "emitido por este serviço no cabeçalho Authorization (bearerAuth).",
                )
                .version(build.version)
                .contact(Contact().name("Solaria").url("https://github.com/Solierrr"))
                .license(License().name("CC0-1.0").url("https://creativecommons.org/publicdomain/zero/1.0/")),
        )
        .servers(
            listOf(
                Server().url("https://api-auth-xw6o.onrender.com").description("QA"),
                Server().url("http://localhost:8081").description("Local"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList("bearerAuth"))

    @Bean
    @Profile("!openapi-export")
    fun hideInternalRoutes(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        openApi.paths.keys.removeIf { it.startsWith("/internal/") }
        val used = openApi.paths.values
            .flatMap { path -> path.readOperations() }
            .flatMap { operation -> operation.tags.orEmpty() }
            .toSet()
        openApi.tags?.removeIf { it.name !in used }
    }

    // A ordem das tags segue a dos beans, que muda entre máquinas; ordenada, o contrato exportado não muda à toa
    @Bean
    fun sortTags(): OpenApiCustomizer = OpenApiCustomizer { openApi -> openApi.tags?.sortBy { it.name } }

    // Toda resposta 4xx/5xx documentada usa o ProblemDetail do contrato de erro; toda operação documenta o 500
    @Bean
    fun problemDetailResponses(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        if (openApi.components == null) openApi.components = Components()
        ModelConverters.getInstance().readAll(AnnotatedType(ProblemDetailSchema::class.java))
            .forEach { (name, schema) -> openApi.components.addSchemas(name, schema) }
        val problem = Content().addMediaType(
            PROBLEM_JSON,
            MediaType().schema(Schema<Any>().`$ref`("#/components/schemas/${ProblemDetailSchema.NAME}")),
        )
        openApi.paths.values.forEach { path ->
            path.readOperations().forEach { operation ->
                operation.responses.putIfAbsent("500", ApiResponse().description("Erro interno inesperado"))
                operation.responses.forEach { (code, response) ->
                    if (code.startsWith("4") || code.startsWith("5")) response.content = problem
                }
            }
        }
    }

    private companion object {
        const val PROBLEM_JSON = "application/problem+json"
    }
}
