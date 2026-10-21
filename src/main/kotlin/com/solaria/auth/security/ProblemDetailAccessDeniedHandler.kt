package com.solaria.auth.security

import com.solaria.auth.exception.handler.ProblemDetailFactory
import com.solaria.auth.observability.HttpObservationErrors
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ProblemDetailAccessDeniedHandler(
    private val factory: ProblemDetailFactory,
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, exception: AccessDeniedException) {
        HttpObservationErrors.mark(request, exception)
        log.atWarn().addKeyValue("errorCode", "ACCESS_DENIED")
            .log("403 em {} {}: {}", request.method, request.requestURI, exception.message)
        val body = factory.create(HttpStatus.FORBIDDEN, "Access is denied", "ACCESS_DENIED",
            mapOf("code" to "ACCESS_DENIED", "message" to "Access is denied"), request)
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = "application/problem+json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
