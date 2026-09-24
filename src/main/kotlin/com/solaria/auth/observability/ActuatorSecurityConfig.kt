package com.solaria.auth.observability

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

/**
 * `SecurityFilterChain` dedicada e prioritária (`@Order(0)`) para os endpoints do Actuator
 *
 * Esta chain garante que só `/actuator/health` e `/actuator/info` fiquem públicos e o resto retorne `401`
 * Classe separada de `SecurityConfig`
 */
@Configuration
class ActuatorSecurityConfig {

    @Bean
    @Order(0)
    fun actuatorSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                    .anyRequest().denyAll()
            }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        return http.build()
    }
}
