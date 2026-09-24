package com.solaria.auth.controller

import com.solaria.auth.dto.auth.request.FirebaseAccountLinkRequest
import com.solaria.auth.dto.auth.request.FirebaseLoginRequest
import com.solaria.auth.dto.auth.request.LoginRequest
import com.solaria.auth.dto.auth.request.RefreshTokenRequest
import com.solaria.auth.dto.auth.request.RegisterRequest
import com.solaria.auth.dto.auth.response.AuthResponse
import com.solaria.auth.dto.auth.response.RegisterResponse
import com.solaria.auth.security.AuthUser
import com.solaria.auth.service.AuthService
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.FirebaseAuthenticationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Endpoints de registro, login e gerenciamento de sessão")
class AuthController(
    private val authService: AuthService,
    private val firebaseAuthenticationService: FirebaseAuthenticationService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra um novo usuário")
    fun register(@Valid @RequestBody request: RegisterRequest): RegisterResponse {
        val user = authService.register(request.email, request.password)
        return RegisterResponse(requireNotNull(user.id), user.primaryEmail, "User registered")
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário com email e senha")
    fun login(@Valid @RequestBody request: LoginRequest, servletRequest: HttpServletRequest): AuthResponse {
        return authService.login(
            request.email,
            request.password,
            servletRequest.remoteAddr,
            servletRequest.getHeader("User-Agent")
        ).toResponse()
    }

    @PostMapping("/firebase")
    @Operation(summary = "Autentica um usuário via Firebase")
    fun firebaseLogin(
        @Valid @RequestBody request: FirebaseLoginRequest,
        servletRequest: HttpServletRequest
    ): AuthResponse = firebaseAuthenticationService.login(
        request.idToken,
        servletRequest.remoteAddr,
        servletRequest.getHeader("User-Agent"),
        request.device
    ).toResponse()

    @PostMapping("/firebase/link")
    @Operation(summary = "Vincula uma conta Firebase a uma conta local existente")
    fun linkFirebaseAccount(
        @Valid @RequestBody request: FirebaseAccountLinkRequest,
        servletRequest: HttpServletRequest
    ): AuthResponse = firebaseAuthenticationService.link(
        request.email,
        request.password,
        request.idToken,
        servletRequest.remoteAddr,
        servletRequest.getHeader("User-Agent"),
        request.device
    ).toResponse()

    @PostMapping("/refresh")
    @Operation(summary = "Gera um novo access token a partir do refresh token")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest, servletRequest: HttpServletRequest): AuthResponse {
        return authService.refresh(
            request.refreshToken,
            servletRequest.remoteAddr,
            servletRequest.getHeader("User-Agent")
        ).toResponse()
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra a sessão do usuário autenticado")
    fun logout(@AuthenticationPrincipal principal: AuthUser) {
        authService.logout(principal.id)
    }

    private fun AuthSession.toResponse() = AuthResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAt = accessTokenExpiresAt,
        userId = userId,
        email = email
    )
}
