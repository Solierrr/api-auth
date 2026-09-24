package com.solaria.auth.observability

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * Liga o appender OTEL do Logback ao [OpenTelemetry] da aplicação
 *
 * sem isto os logs só saem no console
 * Se o [OpenTelemetry] não estiver disponível, a aplicação segue normal e o appender OTEL fica inutilzado
 */
@Component
class OpenTelemetryAppenderInitializer(
    private val openTelemetryProvider: ObjectProvider<OpenTelemetry>,
) : ApplicationListener<ApplicationReadyEvent> {

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        openTelemetryProvider.ifAvailable?.let { OpenTelemetryAppender.install(it) }
    }
}
