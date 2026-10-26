package com.solaria.auth

import com.nimbusds.jose.jwk.JWKSet
import com.solaria.auth.controller.AuthController
import com.solaria.auth.controller.EmailCredentialController
import com.solaria.auth.controller.JwksController
import com.solaria.auth.exception.handler.ProblemDetailFactory
import com.solaria.auth.mobile.MobileAuthorizationService
import com.solaria.auth.mobile.MobileController
import com.solaria.auth.mobile.SessionController
import com.solaria.auth.repository.SessionRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.JwtAuthenticationFilter
import com.solaria.auth.security.config.OpenApiConfig
import com.solaria.auth.service.AuthService
import com.solaria.auth.service.EmailCredentialService
import com.solaria.auth.service.FirebaseAuthenticationService
import com.solaria.auth.service.FirebasePasswordMigrationService
import com.solaria.auth.service.SessionService
import org.junit.jupiter.api.Test
import org.springdoc.core.configuration.SpringDocConfiguration
import org.springdoc.core.configuration.SpringDocJacksonKotlinModuleConfiguration
import org.springdoc.core.configuration.SpringDocKotlinConfiguration
import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.core.properties.SwaggerUiOAuthProperties
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration
import org.springdoc.webmvc.ui.SwaggerConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

// Gera o openapi.yaml versionado (make openapi) só com a camada web: sem banco, Redis, keystore nem Firebase
@WebMvcTest(
    controllers = [
        AuthController::class,
        EmailCredentialController::class,
        JwksController::class,
        MobileController::class,
        SessionController::class,
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("openapi-export")
@Import(OpenApiConfig::class, ProblemDetailFactory::class)
@ImportAutoConfiguration(
    ProjectInfoAutoConfiguration::class,
    SpringDocConfiguration::class,
    SpringDocConfigProperties::class,
    SpringDocKotlinConfiguration::class,
    SpringDocJacksonKotlinModuleConfiguration::class,
    SpringDocWebMvcConfiguration::class,
    SwaggerConfig::class,
    SwaggerUiConfigProperties::class,
    SwaggerUiOAuthProperties::class,
)
class OpenApiExportTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean lateinit var authService: AuthService

    @MockitoBean lateinit var firebaseAuthenticationService: FirebaseAuthenticationService

    @MockitoBean lateinit var emailCredentialService: EmailCredentialService

    @MockitoBean lateinit var firebasePasswordMigrationService: FirebasePasswordMigrationService

    @MockitoBean lateinit var jwkSet: JWKSet

    @MockitoBean lateinit var mobileAuthorizationService: MobileAuthorizationService

    @MockitoBean lateinit var sessionRepository: SessionRepository

    @MockitoBean lateinit var userAccountRepository: UserAccountRepository

    @MockitoBean lateinit var sessionService: SessionService

    @MockitoBean lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @Test
    fun `exports the contract`() {
        val yaml = mockMvc.perform(get("/openapi.json.yaml"))
            .andExpect(status().isOk)
            .andReturn().response.getContentAsString(StandardCharsets.UTF_8)
            .replace("\r\n", "\n")

        assertTrue(yaml.contains("/auth/login:"))

        // o release-please atualiza a versão por esta marca, sem reformatar o arquivo
        val contract = yaml.replaceFirst(Regex("(?m)^(  version: \\d+\\.\\d+\\.\\d+)$"), "$1 # x-release-please-version")
        assertTrue(contract.contains("# x-release-please-version"))
        Files.writeString(CONTRACT, contract, StandardCharsets.UTF_8)
    }

    private companion object {
        val CONTRACT: Path = Path.of("openapi.yaml")
    }
}
