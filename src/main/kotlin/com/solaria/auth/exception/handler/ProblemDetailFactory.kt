package com.solaria.auth.exception.handler

import io.opentelemetry.api.trace.Span
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Component
import java.net.URI
import java.time.Instant

@Component
class ProblemDetailFactory {
    fun create(
        status: HttpStatusCode,
        detail: String,
        errorCode: String,
        properties: Map<String, Any> = emptyMap(),
        request: HttpServletRequest? = null,
    ): ProblemDetail = ProblemDetail.forStatusAndDetail(status, detail).apply {
        type = ABOUT_BLANK
        title = errorCode
        if (request != null) instance = URI.create(request.requestURI)
        setProperty("errorCode", errorCode)
        setProperty("timestamp", Instant.now())
        val span = Span.current().spanContext
        if (span.isValid) setProperty("traceId", span.traceId)
        properties.forEach { (key, value) -> setProperty(key, value) }
    }

    private companion object {
        val ABOUT_BLANK: URI = URI.create("about:blank")
    }
}
