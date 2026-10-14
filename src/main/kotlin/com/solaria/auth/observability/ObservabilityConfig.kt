package com.solaria.auth.observability

import io.micrometer.observation.ObservationPredicate
import io.micrometer.tracing.Tracer
import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.task.TaskDecorator
import org.springframework.core.task.support.ContextPropagatingTaskDecorator
import org.springframework.http.server.observation.ServerRequestObservationContext

/**
 * Configuração central de observabilidade
 *
 * Concentra na camada de infra os beans que ajustam Micrometer / Micrometer Tracing / OpenTelemetry,
 * sem misturar observabilidade com negócio
 */
@Configuration
class ObservabilityConfig {

    @Bean
    fun traceIdResponseFilterRegistration(
        tracerProvider: ObjectProvider<Tracer>,
    ): FilterRegistrationBean<TraceIdResponseFilter> {
        val registration = FilterRegistrationBean(TraceIdResponseFilter(tracerProvider))
        registration.order = Ordered.HIGHEST_PRECEDENCE + 2
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR)
        registration.addUrlPatterns("/*")
        return registration
    }

    @Bean
    fun noActuatorObservations(): ObservationPredicate =
        ObservationPredicate { _, context ->
            if (context is ServerRequestObservationContext) {
                val uri = context.carrier?.requestURI
                uri == null || !uri.startsWith("/actuator")
            } else {
                true
            }
        }

    @Bean
    @ConditionalOnClass(name = ["io.micrometer.context.ContextSnapshot"])
    fun contextPropagatingTaskDecorator(): TaskDecorator = ContextPropagatingTaskDecorator()
}
