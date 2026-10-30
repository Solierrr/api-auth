package com.solaria.auth.mobile

import com.solaria.auth.security.AuthUser
import com.solaria.auth.exception.handler.ProblemDetailFactory
import com.solaria.auth.observability.HttpObservationErrors
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.slf4j.LoggerFactory

data class MobileConfirmation(val confirmed: Boolean)
data class MobileCallback(val callbackUrl: String)

@RestController
@RequestMapping("/auth/mobile")
@Tag(name = "App mobile", description = "Autorização do app mobile pela sessão web (PKCE)")
class MobileController(private val service: MobileAuthorizationService) {
    @PostMapping("/transactions")
    @Operation(
        operationId = "startMobileAuthorization",
        summary = "Inicia a autorização do app mobile",
        description = "Fluxo com PKCE (S256): devolve a URL da web onde a pessoa entra e confirma o acesso do app.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"MALFORMED_REQUEST\", \"INVALID_CLIENT\", \"INVALID_TRANSACTION\", \"MOBILE_AUTH_NOT_CONFIGURED\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Transação criada"),
        ApiResponse(responseCode = "400", description = "Cliente, redirect ou parâmetros do PKCE inválidos"),
        ApiResponse(responseCode = "422", description = "Corpo ilegível"),
        ApiResponse(responseCode = "503", description = "Autorização do app não configurada"),
    )
    fun start(@RequestBody request: StartMobile) = privateResponse(service.start(request))

    @GetMapping("/transactions/{id}")
    @Operation(
        operationId = "getMobileAuthorization",
        summary = "Consulta a transação de autorização do app",
        description = "Usada pela web, com a sessão da pessoa, para mostrar o pedido do app e se a conta pode usá-lo.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"UNAUTHENTICATED\", \"INVALID_TRANSACTION\", \"TRANSACTION_EXPIRED\", \"INVALID_GRANT\", \"ELIGIBILITY_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Dados da transação"),
        ApiResponse(responseCode = "400", description = "Transação inválida ou expirada, ou sessão web inválida"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "503", description = "Consulta de elegibilidade indisponível"),
    )
    fun inspect(@PathVariable id: String, @AuthenticationPrincipal principal: AuthUser) =
        privateResponse(service.inspect(id, principal))

    @PostMapping("/transactions/{id}/authorize")
    @Operation(
        operationId = "authorizeMobile",
        summary = "Autoriza o app mobile",
        description = "Exige confirmação explícita, sessão web, e-mail verificado e conta elegível. Devolve a URL de retorno do app com o código de autorização.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"MALFORMED_REQUEST\", \"UNAUTHENTICATED\", \"INVALID_TRANSACTION\", \"TRANSACTION_EXPIRED\", \"INVALID_GRANT\", \"CONFIRMATION_REQUIRED\", \"WEB_SESSION_REQUIRED\", \"EMAIL_NOT_VERIFIED\", \"MOBILE_ACCOUNT_NOT_ALLOWED\", \"ELIGIBILITY_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "URL de retorno com o código"),
        ApiResponse(responseCode = "400", description = "Transação inválida, expirada ou sem confirmação"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Sessão não é web, e-mail não verificado ou conta sem permissão para o app"),
        ApiResponse(responseCode = "422", description = "Corpo ilegível"),
        ApiResponse(responseCode = "503", description = "Consulta de elegibilidade indisponível"),
    )
    fun authorize(@PathVariable id: String, @AuthenticationPrincipal principal: AuthUser,
        @RequestBody request: MobileConfirmation) =
        privateResponse(MobileCallback(service.authorize(id, principal, request.confirmed)))

    @DeleteMapping("/transactions/{id}")
    @Operation(
        operationId = "cancelMobileAuthorization",
        summary = "Cancela a transação de autorização do app",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"UNAUTHENTICATED\", \"INVALID_TRANSACTION\", \"TRANSACTION_EXPIRED\", \"INVALID_GRANT\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Transação cancelada"),
        ApiResponse(responseCode = "400", description = "Transação inválida ou expirada, ou sessão web inválida"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
    )
    fun cancel(@PathVariable id: String, @AuthenticationPrincipal principal: AuthUser): ResponseEntity<Void> {
        service.cancel(id, principal)
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build()
    }

    @PostMapping("/token")
    @Operation(
        operationId = "redeemMobileToken",
        summary = "Troca o código de autorização pelos tokens do app",
        description = "Confere o code_verifier do PKCE e abre a sessão mobile (contexto professional).",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"MALFORMED_REQUEST\", \"INVALID_GRANT\", \"MOBILE_ACCOUNT_NOT_ALLOWED\", \"ELIGIBILITY_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Sessão mobile aberta"),
        ApiResponse(responseCode = "400", description = "Código de autorização inválido"),
        ApiResponse(responseCode = "403", description = "Conta sem permissão para o app"),
        ApiResponse(responseCode = "422", description = "Corpo ilegível"),
        ApiResponse(responseCode = "503", description = "Consulta de elegibilidade indisponível"),
    )
    fun redeem(@RequestBody request: MobileGrant, servlet: HttpServletRequest) =
        privateResponse(service.redeem(request, servlet.remoteAddr, servlet.getHeader("User-Agent")))

    private fun <T : Any> privateResponse(body: T) = ResponseEntity.ok()
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("Referrer-Policy", "no-referrer").body(body)
}

@RestControllerAdvice
@org.springframework.core.annotation.Order(-1)
class MobileExceptionHandler(private val factory: ProblemDetailFactory) {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MobileFlowException::class)
    fun handle(exception: MobileFlowException, request: HttpServletRequest): ResponseEntity<org.springframework.http.ProblemDetail> {
        HttpObservationErrors.mark(request, exception)
        val event = if (exception.status.is5xxServerError) log.atError().setCause(exception) else log.atWarn()
        event.addKeyValue("errorCode", exception.code)
            .log("Erro {} em {} {}", exception.status.value(), request.method, request.requestURI)
        val body = factory.create(exception.status, "The mobile authorization request could not be processed",
            exception.code, mapOf("code" to exception.code), request)
        return ResponseEntity.status(exception.status).header(HttpHeaders.CACHE_CONTROL, "no-store").body(body)
    }
}
