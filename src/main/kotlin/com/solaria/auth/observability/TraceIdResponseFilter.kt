package com.solaria.auth.observability

import io.micrometer.tracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.ObjectProvider
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Copia o `traceId` do span ativo para o header de resposta `X-Trace-Id`
 *
 * O header é escrito na entrada
 * Cobre 2xx, 4xx/5xx do advice, 401/403 da segurança e o dispatch `/error`
 * Sem [Tracer] (tracing desligado) ou sem span ativo -> ignora
 *
 */
class TraceIdResponseFilter(
    private val tracerProvider: ObjectProvider<Tracer>,
) : OncePerRequestFilter() {

    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val tracer = tracerProvider.ifAvailable
        if (tracer != null && !response.isCommitted) {
            tracer.currentSpan()?.let { span ->
                response.setHeader(TRACE_ID_HEADER, span.context().traceId())
            }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        /** Header de resposta com o `traceId`. */
        const val TRACE_ID_HEADER = "X-Trace-Id"
    }
}
