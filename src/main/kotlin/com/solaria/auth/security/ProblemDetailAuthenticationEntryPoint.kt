package com.solaria.auth.security

import com.solaria.auth.exception.handler.ProblemDetailFactory
import com.solaria.auth.observability.HttpObservationErrors
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ProblemDetailAuthenticationEntryPoint(
    private val factory: ProblemDetailFactory,
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun commence(request: HttpServletRequest, response: HttpServletResponse, exception: AuthenticationException) {
        HttpObservationErrors.mark(request, exception)
        log.atWarn().addKeyValue("errorCode", "UNAUTHENTICATED")
            .log("401 em {} {}: {}", request.method, request.requestURI, exception.message)
        val body = factory.create(HttpStatus.UNAUTHORIZED, "Authentication is required", "UNAUTHENTICATED",
            mapOf("code" to "UNAUTHENTICATED", "message" to "Authentication is required"), request)
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = "application/problem+json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
