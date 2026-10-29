package com.solaria.auth.security.config

import com.solaria.auth.security.ProblemDetailAuthenticationEntryPoint
import com.solaria.auth.security.ServiceKeyAuthFilter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter

@Configuration
@EnableConfigurationProperties(ServiceKeyProperties::class)
class ServiceKeySecurityConfig(private val problemDetailAuthenticationEntryPoint: ProblemDetailAuthenticationEntryPoint) {

    @Bean
    @Order(1)
    fun serviceKeySecurityFilterChain(http: HttpSecurity, serviceKeyAuthFilter: ServiceKeyAuthFilter): SecurityFilterChain = http
        .securityMatcher("/internal/technicians/**")
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .addFilterBefore(serviceKeyAuthFilter, AnonymousAuthenticationFilter::class.java)
        .authorizeHttpRequests { it.anyRequest().hasRole("SERVICE") }
        .exceptionHandling {
            it.authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            it.accessDeniedHandler { request, response, exception ->
                problemDetailAuthenticationEntryPoint.commence(request, response,
                    InsufficientAuthenticationException("Service key is missing or invalid", exception))
            }
        }
        .build()

    @Bean
    fun serviceKeyAuthFilterRegistration(filter: ServiceKeyAuthFilter): FilterRegistrationBean<ServiceKeyAuthFilter> =
        FilterRegistrationBean(filter).apply { isEnabled = false }
}
