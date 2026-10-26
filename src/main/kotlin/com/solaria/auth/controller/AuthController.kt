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
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
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
@Tag(name = "Autenticação", description = "Cadastro, login com e-mail e senha ou pelo Firebase, renovação e encerramento da sessão")
class AuthController(
    private val authService: AuthService,
    private val firebaseAuthenticationService: FirebaseAuthenticationService
) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        operationId = "register",
        summary = "Registra um novo usuário",
        description = "Cria a conta com e-mail e senha local (12 a 72 bytes). O perfil no api-core é criado em seguida, por evento.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"PASSWORD_POLICY\", \"EMAIL_ALREADY_REGISTERED\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Conta criada"),
        ApiResponse(responseCode = "400", description = "Senha fora da política"),
        ApiResponse(responseCode = "409", description = "E-mail já cadastrado"),
        ApiResponse(responseCode = "422", description = "Corpo inválido"),
    )
    fun register(@Valid @RequestBody request: RegisterRequest): RegisterResponse {
        val user = authService.register(request.email, request.password)
        return RegisterResponse(requireNotNull(user.id), user.primaryEmail, "User registered")
    }

    @PostMapping("/login")
    @Operation(
        operationId = "login",
        summary = "Autentica um usuário com email e senha",
        description = "Abre uma sessão web e devolve o access token (JWT) e o refresh token.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"INVALID_CREDENTIALS\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sessão aberta"),
        ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos, ou conta bloqueada"),
        ApiResponse(responseCode = "422", description = "Corpo inválido"),
    )
    fun login(@Valid @RequestBody request: LoginRequest, servletRequest: HttpServletRequest): AuthResponse {
        return authService.login(
            request.email,
            request.password,
            servletRequest.remoteAddr,
            servletRequest.getHeader("User-Agent")
        ).toResponse()
    }

    @PostMapping("/firebase")
    @Operation(
        operationId = "loginWithFirebase",
        summary = "Autentica um usuário via Firebase",
        description = "Valida o ID token do Firebase e abre uma sessão. Cria a conta no primeiro acesso; se já existe conta local com o mesmo e-mail, exige a vinculação.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"INVALID_FIREBASE_TOKEN\", \"ACCOUNT_UNAVAILABLE\", \"ACCOUNT_LINK_REQUIRED\", \"FEDERATED_IDENTITY_CONFLICT\", \"VERIFIED_FIREBASE_EMAIL_REQUIRED\", \"FIREBASE_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sessão aberta"),
        ApiResponse(responseCode = "401", description = "ID token do Firebase inválido"),
        ApiResponse(responseCode = "403", description = "Conta indisponível"),
        ApiResponse(responseCode = "409", description = "Vinculação necessária ou identidade já vinculada a outra conta"),
        ApiResponse(responseCode = "422", description = "Corpo inválido ou e-mail do Firebase não verificado"),
        ApiResponse(responseCode = "503", description = "Firebase indisponível"),
    )
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
    @Operation(
        operationId = "linkFirebaseAccount",
        summary = "Vincula uma conta Firebase a uma conta local existente",
        description = "Confere o e-mail e a senha da conta local e o ID token do Firebase, vincula a identidade e abre uma sessão.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"INVALID_FIREBASE_TOKEN\", \"INVALID_ACCOUNT_LINK_CREDENTIALS\", \"ACCOUNT_UNAVAILABLE\", \"FIREBASE_ACCOUNT_LINK_MISMATCH\", \"FEDERATED_IDENTITY_CONFLICT\", \"VERIFIED_FIREBASE_EMAIL_REQUIRED\", \"FIREBASE_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Conta vinculada e sessão aberta"),
        ApiResponse(responseCode = "401", description = "Credenciais da conta local ou ID token inválidos"),
        ApiResponse(responseCode = "403", description = "Conta indisponível"),
        ApiResponse(responseCode = "409", description = "E-mail do Firebase diferente da conta ou identidade já vinculada"),
        ApiResponse(responseCode = "422", description = "Corpo inválido ou e-mail do Firebase não verificado"),
        ApiResponse(responseCode = "503", description = "Firebase indisponível"),
    )
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
    @Operation(
        operationId = "refreshToken",
        summary = "Gera um novo access token a partir do refresh token",
        description = "Troca o refresh token por um novo par (rotação a cada uso). O reuso de um refresh token já trocado revoga a sessão.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"INVALID_REFRESH_TOKEN\", \"REFRESH_TOKEN_REUSED\", \"ACCOUNT_UNAVAILABLE\", \"MOBILE_ACCOUNT_NOT_ALLOWED\", \"ELIGIBILITY_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Tokens renovados"),
        ApiResponse(responseCode = "401", description = "Refresh token inválido ou reutilizado"),
        ApiResponse(responseCode = "403", description = "Conta indisponível ou sem permissão para o app"),
        ApiResponse(responseCode = "422", description = "Corpo inválido"),
        ApiResponse(responseCode = "503", description = "Consulta de elegibilidade do app indisponível"),
    )
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest, servletRequest: HttpServletRequest): AuthResponse {
        return authService.refresh(
            request.refreshToken,
            servletRequest.remoteAddr,
            servletRequest.getHeader("User-Agent")
        ).toResponse()
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        operationId = "logout",
        summary = "Encerra a sessão do usuário autenticado",
        description = "Revoga todos os refresh tokens do usuário.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Sessão encerrada"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
    )
    fun logout(@AuthenticationPrincipal principal: AuthUser) {
        authService.logout(principal.id)
    }

    private fun AuthSession.toResponse() = AuthResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        accessTokenExpiresAt = accessTokenExpiresAt,
        userId = userId,
        email = email,
        sessionId = sessionId,
        clientId = clientId,
        context = context
    )
}
