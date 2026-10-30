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

/**
 * Devolve o 401 de autenticação no formato [ProblemDetail] (`UNAUTHENTICATED`)
 *
 * <p>
 * Chamado pelo Spring Security quando o token está ausente, inválido ou expirado -> roda antes do Spring MVC,
 * por isso a resposta é escrita direto no [HttpServletResponse]
 * </p>
 *
 * <p>Marca o span como erro e loga `WARN`, igual aos demais 4xx</p>
 */
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
        val message = "Autenticação necessária: token ausente, inválido ou expirado."
        val body = factory.create(HttpStatus.UNAUTHORIZED, message, "UNAUTHENTICATED",
            mapOf("code" to "UNAUTHENTICATED", "message" to message), request)
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = "application/problem+json"
        response.characterEncoding = "UTF-8"
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
