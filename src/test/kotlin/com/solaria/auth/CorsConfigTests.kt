package com.solaria.auth

import com.solaria.auth.security.config.CorsConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CorsConfigTests {
    private fun policy(origins: String) = CorsConfig(origins).corsConfigurationSource()
        .getCorsConfiguration(MockHttpServletRequest("OPTIONS", "/auth/password/reset/request"))

    @Test
    fun `the browser may call the recovery routes from the listed web origins only`() {
        val cors = requireNotNull(policy("https://app.example.com, https://qa.example.com"))

        assertEquals("https://app.example.com", cors.checkOrigin("https://app.example.com"))
        assertEquals("https://qa.example.com", cors.checkOrigin("https://qa.example.com"))
        assertEquals(null, cors.checkOrigin("https://evil.example.com"))
        assertTrue(HttpMethod.POST in requireNotNull(cors.checkHttpMethod(HttpMethod.POST)))
        assertEquals(null, cors.checkHttpMethod(HttpMethod.PUT))
        assertEquals(listOf("Content-Type"), cors.checkHeaders(listOf("Content-Type")))
        assertEquals(null, cors.checkHeaders(listOf("X-Service-Key")))
    }

    @Test
    fun `the shared cors variable is used when the auth specific one is missing`() {
        fun origins(vararg values: String) = ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withPropertyValues(*values)
        origins("CORS_ALLOWED_ORIGINS=https://solarianetwork.site").run {
            assertEquals("https://solarianetwork.site", it.environment.getProperty("app.cors.allowed-origins"))
        }
        origins("AUTH_CORS_ALLOWED_ORIGINS=https://qa.example.com", "CORS_ALLOWED_ORIGINS=https://solarianetwork.site").run {
            assertEquals("https://qa.example.com", it.environment.getProperty("app.cors.allowed-origins"))
        }
    }
}
