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

/**
 * Devolve o 403 de RBAC no formato [ProblemDetail] (`ACCESS_DENIED`)
 *
 * <p>
 * Roda na chain do Spring Security, antes do Spring MVC -> o `AuthExceptionHandler` não alcança este erro,
 * por isso a resposta é escrita direto no [HttpServletResponse]
 * </p>
 *
 * <p>Marca o span como erro e loga `WARN`, igual aos demais 4xx</p>
 */
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
        val message = "Acesso negado: você não tem permissão para acessar este recurso."
        val body = factory.create(HttpStatus.FORBIDDEN, message, "ACCESS_DENIED",
            mapOf("code" to "ACCESS_DENIED", "message" to message), request)
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = "application/problem+json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
