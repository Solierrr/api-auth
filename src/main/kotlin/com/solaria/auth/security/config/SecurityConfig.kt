package com.solaria.auth.security.config

import com.solaria.auth.security.AccountUserDetailsService

import com.solaria.auth.security.JwtAuthenticationFilter
import com.solaria.auth.security.ProblemDetailAuthenticationEntryPoint
import com.solaria.auth.security.ProblemDetailAccessDeniedHandler
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val accountUserDetailsService: AccountUserDetailsService,
    private val problemDetailAuthenticationEntryPoint: ProblemDetailAuthenticationEntryPoint,
    private val problemDetailAccessDeniedHandler: ProblemDetailAccessDeniedHandler,
) {

    /**
     * Fábrica da única SecurityFilterChain deste serviço
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        // CSRF desabilitado
        .csrf { it.disable() }
        .cors { }
        // Nunca cria/usa HttpSession
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        // Registra o provider de autenticação (login local email+senha) usado pelos fluxos de login.
        .authenticationProvider(authenticationProvider())
        // Customiza o corpo das respostas 401/403 para um JSON
        // observacao do servidor como erro + log WARN correlacionado ao trace
        .exceptionHandling {
            // Sem autenticação válida -> responde 401 com um JSON
            it.authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            // Autenticado, mas sem permissão para o recurso -> responde 403 com um JSON
            it.accessDeniedHandler(problemDetailAccessDeniedHandler)
        }
        .authorizeHttpRequests {
            // Endpoints de emissão de credencial ficam públicos
            it.requestMatchers(
                HttpMethod.POST,
                "/auth/register",
                "/auth/login",
                "/auth/firebase",
                "/auth/firebase/link",
                "/auth/refresh",
                "/auth/mobile/transactions",
                "/auth/mobile/token",
                "/auth/email/verification/confirm",
                "/auth/password/reset/request",
                "/auth/password/reset/confirm"
            ).permitAll()
            // /actuator/** e tratado antes por observability/ActuatorSecurityConfig
            // health/info liberados, resto denyAll
            // Endpoint JWKS público
            it.requestMatchers(HttpMethod.GET, "/internal/sessions/*", "/.well-known/jwks.json").permitAll()
            // Rotas consumidas apenas pela rede interna do cluster.
            it.requestMatchers("/internal/**").permitAll()
            // Docs OpenAPI/Swagger ficam públicas
            it.requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
            ).permitAll()
            // o resto exige autenticação
            it.anyRequest().authenticated()
        }
        // Posiciona o filtro de autenticação por JWT antes do filtro de referência de form login
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        // Constrói e devolve a SecurityFilterChain configurada acima como o bean deste método.
        .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider = DaoAuthenticationProvider(accountUserDetailsService).apply {
        setPasswordEncoder(passwordEncoder())
    }

    @Bean
    fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager =
        configuration.authenticationManager
}
