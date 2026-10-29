package com.solaria.auth

import com.solaria.auth.calendar.GoogleCalendarTokenController
import com.solaria.auth.calendar.GoogleCalendarTokenService
import com.solaria.auth.calendar.GoogleCalendarTokenStatus
import com.solaria.auth.calendar.GoogleCalendarTokens
import com.solaria.auth.controller.AuthExceptionHandler
import com.solaria.auth.exception.handler.ProblemDetailFactory
import com.solaria.auth.repository.SessionRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.JwtService
import com.solaria.auth.security.ProblemDetailAuthenticationEntryPoint
import com.solaria.auth.security.ServiceKeyAuthFilter
import com.solaria.auth.security.config.ServiceKeySecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.server.ResponseStatusException

@WebMvcTest(GoogleCalendarTokenController::class)
@Import(
    ServiceKeySecurityConfig::class,
    ServiceKeyAuthFilter::class,
    ProblemDetailAuthenticationEntryPoint::class,
    ProblemDetailFactory::class,
    AuthExceptionHandler::class
)
@TestPropertySource(properties = ["app.service-keys.google-registry=$SERVICE_KEY"])
class GoogleCalendarTokenControllerTests {
    @Autowired
    lateinit var mockMvc: MockMvc

    @MockitoBean
    lateinit var service: GoogleCalendarTokenService

    @MockitoBean
    lateinit var jwtService: JwtService

    @MockitoBean
    lateinit var userAccountRepository: UserAccountRepository

    @MockitoBean
    lateinit var sessionRepository: SessionRepository

    @Test
    fun `missing or wrong service key is rejected`() {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized)
        mockMvc.perform(get(URL).header("X-Service-Key", "x".repeat(40))).andExpect(status().isUnauthorized)
        mockMvc.perform(get(URL).header("Authorization", "Bearer user-token")).andExpect(status().isUnauthorized)
        Mockito.verifyNoInteractions(service)
    }

    @Test
    fun `get returns connected tokens without caching`() {
        Mockito.`when`(service.connected("tech-1"))
            .thenReturn(GoogleCalendarTokens("access", "refresh", GoogleCalendarTokenStatus.CONNECTED))

        mockMvc.perform(get(URL).header("X-Service-Key", SERVICE_KEY))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.accessToken").value("access"))
            .andExpect(jsonPath("$.refreshToken").value("refresh"))
            .andExpect(jsonPath("$.status").value("CONNECTED"))
    }

    @Test
    fun `get answers not found when the technician is not connected`() {
        Mockito.`when`(service.connected("tech-1")).thenThrow(ResponseStatusException(HttpStatus.NOT_FOUND))

        mockMvc.perform(get(URL).header("X-Service-Key", SERVICE_KEY)).andExpect(status().isNotFound)
    }

    @Test
    fun `put stores the token pair`() {
        mockMvc.perform(put(URL).header("X-Service-Key", SERVICE_KEY).contentType(MediaType.APPLICATION_JSON)
            .content("""{"accessToken":"access","refreshToken":"refresh"}"""))
            .andExpect(status().isNoContent)

        Mockito.verify(service).save("tech-1", "access", "refresh")
    }

    @Test
    fun `put without both tokens is a bad request`() {
        mockMvc.perform(put(URL).header("X-Service-Key", SERVICE_KEY).contentType(MediaType.APPLICATION_JSON)
            .content("""{"accessToken":"access","refreshToken":" "}"""))
            .andExpect(status().isBadRequest)

        Mockito.verifyNoInteractions(service)
    }

    @Test
    fun `delete disconnects and patch revokes`() {
        mockMvc.perform(delete(URL).header("X-Service-Key", SERVICE_KEY)).andExpect(status().isNoContent)
        mockMvc.perform(patch("$URL/revoke").header("X-Service-Key", SERVICE_KEY)).andExpect(status().isNoContent)

        Mockito.verify(service).disconnect("tech-1")
        Mockito.verify(service).revoke("tech-1")
    }

    @Test
    fun `missing encryption key answers service unavailable`() {
        Mockito.doThrow(ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
            .`when`(service).save("tech-1", "access", "refresh")

        mockMvc.perform(put(URL).header("X-Service-Key", SERVICE_KEY).contentType(MediaType.APPLICATION_JSON)
            .content("""{"accessToken":"access","refreshToken":"refresh"}"""))
            .andExpect(status().isServiceUnavailable)
    }
}

private const val URL = "/internal/technicians/tech-1/google-calendar-token"
private const val SERVICE_KEY = "google-registry-test-key-0123456789abcdef"
