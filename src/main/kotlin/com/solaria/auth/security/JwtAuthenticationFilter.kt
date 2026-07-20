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

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userAccountRepository: UserAccountRepository,
    private val sessionRepository: SessionRepository
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = request.getHeader(AUTHORIZATION_HEADER)
            ?.takeIf { it.startsWith(BEARER_PREFIX) }
            ?.removePrefix(BEARER_PREFIX)

        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            authenticate(token, request)
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticate(token: String, request: HttpServletRequest) {
        runCatching {
            jwtService.extractAccessTokenIdentity(token)
        }.getOrNull()?.takeIf { identity ->
            sessionRepository.findById(identity.sessionId).orElse(null)?.let { session ->
                session.revokedAt == null &&
                    session.expiresAt.isAfter(java.time.Instant.now()) &&
                    session.user?.id == identity.userId
            } == true
        }?.let { identity ->
            userAccountRepository.findById(identity.userId).orElse(null)
                ?.takeIf { it.status == AccountStatus.ACTIVE && (it.lockedUntil == null || it.lockedUntil!!.isBefore(java.time.Instant.now())) }
                ?.let(AuthUser::fromBearerToken)?.let { user ->
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
