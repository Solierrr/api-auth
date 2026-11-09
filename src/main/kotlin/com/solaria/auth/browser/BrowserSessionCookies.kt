package com.solaria.auth.browser

import com.solaria.auth.security.config.JwtProperties
import com.solaria.auth.security.TokenHashing
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.util.HexFormat

@Component
class BrowserSessionCookies(
    properties: JwtProperties,
    @Value("\${app.cors.allowed-origins}") origins: String,
    @Value("\${app.browser-session.secure:true}") private val secure: Boolean,
) {
    private val allowedOrigins = origins.split(',').map(String::trim).filter(String::isNotEmpty).toSet()
    private val lifetime = properties.refreshTokenTtl

    fun requireBrowser(request: HttpServletRequest, response: HttpServletResponse) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store")
        if (request.getHeader("Origin") !in allowedOrigins || request.getHeader("X-Solaria-Session") != "1") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Browser origin not allowed")
        }
    }

    fun read(request: HttpServletRequest): String? = request.cookies
        ?.singleOrNull { it.name == name(request) }?.value?.takeIf { it.isNotBlank() }

    fun write(request: HttpServletRequest, response: HttpServletResponse, token: String) = set(request, response, token, lifetime)
    fun clear(request: HttpServletRequest, response: HttpServletResponse) = set(request, response, "", Duration.ZERO)

    private fun name(request: HttpServletRequest): String {
        val origin = request.getHeader("Origin") ?: throw ResponseStatusException(HttpStatus.FORBIDDEN, "Browser origin not allowed")
        if (origin !in allowedOrigins) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Browser origin not allowed")
        // Abas compartilham o cookie, mas aplicações de origens distintas não disputam a rotação.
        return NAME + "_" + HexFormat.of().formatHex(TokenHashing.sha256(origin)).take(16)
    }

    private fun set(request: HttpServletRequest, response: HttpServletResponse, token: String, duration: Duration) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name(request), token)
            .httpOnly(true).secure(secure).path("/auth/browser")
            .sameSite(if (secure) "None" else "Lax").partitioned(secure)
            .maxAge(duration).build().toString())
    }

    companion object { const val NAME = "solaria_browser_refresh" }
}
