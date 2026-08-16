package com.solaria.auth.security

import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.repository.SessionRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import com.solaria.auth.enums.AccountStatus


/**
* filtro adicionado em SecurityFilterChain -> intercepta toda requisição para tentar autenticar via Bearer JWT.
 */
// Filtro registrado como bean
@Component
class JwtAuthenticationFilter(
    // Decodifica/valida o token e extrai a identidade (userId/sessionId).
    private val jwtService: JwtService,
    private val userAccountRepository: UserAccountRepository,
    private val sessionRepository: SessionRepository
) : OncePerRequestFilter() {
    public override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Extrai o token do header Authorization só se ele existir e começar com "Bearer "
        // ausência do header (ou prefixo errado) resulta em token == null
        val token = request.getHeader(AUTHORIZATION_HEADER)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)

        // Só tenta autenticar se houver token e ainda não houver autenticação no contexto
        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            authenticate(token, request)
        }

        // Sempre continua a cadeia de filtros, autenticado ou não
        filterChain.doFilter(request, response)
    }

    // Verifica -> token(assinatura RS256, issuer, expiração, token_type) + sessão referenciada + usuario
    private fun authenticate(token: String, request: HttpServletRequest) {
        runCatching {
            // Decodifica e valida o token (assinatura RS256, issuer, expiração, token_type)
            jwtService.extractAccessTokenIdentity(token)
        }.getOrNull()?.takeIf { identity ->
            // Busca a sessão referenciada pelo "sid" do token e confere se ela ainda está ativa
            sessionRepository.findById(identity.sessionId).orElse(null)?.let { session ->
                session.revokedAt == null &&
                    session.expiresAt.isAfter(java.time.Instant.now()) &&
                    session.user?.id == identity.userId
            } == true
        }?.let { identity ->
            // busca o usuário e confere se a conta está ACTIVE
            userAccountRepository.findById(identity.userId).orElse(null)
                ?.takeIf { it.status == AccountStatus.ACTIVE && (it.lockedUntil == null || it.lockedUntil!!.isBefore(java.time.Instant.now())) }
                ?.let(AuthUser::fromBearerToken)?.let { user ->
                // Todas as checagens passaram -> popula o SecurityContext com um Authentication válido
                UsernamePasswordAuthenticationToken(user, null, user.authorities).also { authentication ->
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            }
        }
    }

    private companion object {
        const val AUTHORIZATION_HEADER = "Authorization"
        const val BEARER_PREFIX = "Bearer "
    }
}
