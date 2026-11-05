package com.solaria.auth.security

import com.solaria.auth.security.config.ServiceKeyProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

@Component
class ServiceKeyAuthFilter(private val serviceKeys: ServiceKeyProperties) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val provided = request.getHeader(HEADER)?.takeIf { it.isNotBlank() }?.toByteArray(Charsets.UTF_8)
        if (provided != null) {
            serviceKeys.entries
                .firstOrNull { (_, key) -> key.isNotBlank() && MessageDigest.isEqual(key.toByteArray(Charsets.UTF_8), provided) }
                ?.let { (service, _) ->
                    SecurityContextHolder.getContext().authentication =
                        UsernamePasswordAuthenticationToken(service, null, listOf(SimpleGrantedAuthority("ROLE_SERVICE")))
                }
        }
        filterChain.doFilter(request, response)
    }

    companion object {
        const val HEADER = "X-Service-Key"
    }
}
