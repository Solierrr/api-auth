package com.solaria.auth.exception.handler

import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant

/**
 * Monta o corpo JSON de todo erro da API no formato [ProblemDetail] (RFC 9457)
 *
 * <p>
 * `errorCode` identifica o erro, `timestamp` marca a falha e `traceId` leva direto ao trace no Grafana
 * -> o cliente decide pelo `errorCode`, não pelo `detail`
 * </p>
 *
 * <p>
 * Sem `type` e sem `title` próprio -> o Spring preenche `title` com a reason phrase do status ("Not Found")
 * </p>
 */
@Component
class ProblemDetailFactory {
    fun create(
        status: HttpStatusCode,
        detail: String,
        errorCode: String,
        properties: Map<String, Any> = emptyMap(),
        request: HttpServletRequest? = null,
    ): ProblemDetail = ProblemDetail.forStatusAndDetail(status, detail).apply {
        if (request != null) instance = URI.create(request.requestURI)
        setProperty("errorCode", errorCode)
        setProperty("timestamp", Instant.now())
        // sem span ativo (tracing desligado) -> corpo sai sem traceId
        val span = Span.current().spanContext
        if (span.isValid) setProperty("traceId", span.traceId)
        properties.forEach { (key, value) -> setProperty(key, value) }
    }
}
