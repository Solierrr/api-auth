package com.solaria.auth.browser

import com.solaria.auth.dto.auth.request.FirebaseAccountLinkRequest
import com.solaria.auth.dto.auth.request.FirebaseLoginRequest
import com.solaria.auth.dto.auth.request.LoginRequest
import com.solaria.auth.dto.auth.response.BrowserAuthResponse
import com.solaria.auth.repository.RefreshTokenRepository
import com.solaria.auth.security.TokenHashing
import com.solaria.auth.service.AuthService
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.FirebaseAuthenticationService
import com.solaria.auth.service.InvalidRefreshTokenException
import com.solaria.auth.service.RefreshTokenReuseException
import com.solaria.auth.service.SessionService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/browser")
class BrowserSessionController(
    private val auth: AuthService,
    private val firebase: FirebaseAuthenticationService,
    private val cookies: BrowserSessionCookies,
    private val tokens: RefreshTokenRepository,
    private val sessions: SessionService,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody body: LoginRequest, request: HttpServletRequest, response: HttpServletResponse): BrowserAuthResponse {
        cookies.requireBrowser(request, response)
        return issue(auth.login(body.email, body.password, request.remoteAddr, request.getHeader("User-Agent")), request, response)
    }

    @PostMapping("/firebase")
    fun firebase(@Valid @RequestBody body: FirebaseLoginRequest, request: HttpServletRequest, response: HttpServletResponse): BrowserAuthResponse {
        cookies.requireBrowser(request, response)
        return issue(firebase.login(body.idToken, request.remoteAddr, request.getHeader("User-Agent"), body.device), request, response)
    }

    @PostMapping("/firebase/link")
    fun link(@Valid @RequestBody body: FirebaseAccountLinkRequest, request: HttpServletRequest, response: HttpServletResponse): BrowserAuthResponse {
        cookies.requireBrowser(request, response)
        return issue(firebase.link(body.email, body.password, body.idToken, request.remoteAddr, request.getHeader("User-Agent"), body.device), request, response)
    }

    @PostMapping("/refresh")
    fun refresh(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<BrowserAuthResponse> {
        cookies.requireBrowser(request, response)
        val token = cookies.read(request) ?: return ResponseEntity.noContent().build()
        try {
            return ResponseEntity.ok(issue(auth.refresh(token, request.remoteAddr, request.getHeader("User-Agent")), request, response))
        } catch (error: RuntimeException) {
            if (error is InvalidRefreshTokenException || error is RefreshTokenReuseException) cookies.clear(request, response)
            throw error
        }
    }

    @PostMapping("/logout")
    @Transactional
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Void> {
        cookies.requireBrowser(request, response)
        cookies.clear(request, response)
        cookies.read(request)?.let { value ->
            tokens.findByTokenHash(TokenHashing.sha256(value))?.session?.let { sessions.revoke(it, "BROWSER_LOGOUT") }
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    private fun issue(session: AuthSession, request: HttpServletRequest, response: HttpServletResponse): BrowserAuthResponse {
        cookies.write(request, response, session.refreshToken)
        return BrowserAuthResponse(session.accessToken, session.accessTokenExpiresAt, session.userId,
            session.email, session.sessionId, session.clientId, session.context)
    }
}
