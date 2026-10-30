package com.solaria.auth.controller

import com.solaria.auth.security.AuthUser
import com.solaria.auth.service.EmailCredentialService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class EmailRequest(@field:Email @field:NotBlank @field:Size(max = 254) val email: String)
data class TokenRequest(@field:Size(min = 43, max = 43) val token: String)
data class ResetPasswordRequest(val token: String, val password: String)
data class MigratePasswordRequest(val idToken: String, val password: String)
 data class ChangePasswordRequest(val currentPassword: String, val password: String)

@RestController
@RequestMapping("/auth")
@Tag(name = "Credenciais", description = "Confirmação do e-mail, recuperação, migração e troca de senha")
class EmailCredentialController(private val service: EmailCredentialService,
    private val migration: com.solaria.auth.service.FirebasePasswordMigrationService) {
    @PostMapping("/email/verification/request")
    @Operation(
        operationId = "requestEmailVerification",
        summary = "Envia o e-mail de confirmação para o usuário autenticado",
        description = "Não envia nada se o e-mail já está confirmado.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"UNAUTHENTICATED\", \"ACCOUNT_UNAVAILABLE\", \"EMAIL_NOT_CONFIGURED\", \"EMAIL_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "Pedido aceito"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
        ApiResponse(responseCode = "403", description = "Conta indisponível"),
        ApiResponse(responseCode = "503", description = "Envio de e-mail não configurado ou indisponível"),
    )
    fun requestVerification(@AuthenticationPrincipal principal: AuthUser): ResponseEntity<Map<String, String>> {
        service.requestVerification(principal.id)
        return ResponseEntity.accepted().body(mapOf("status" to "REQUEST_ACCEPTED"))
    }

    @PostMapping("/email/verification/confirm")
    @Operation(
        operationId = "confirmEmailVerification",
        summary = "Confirma o e-mail com o token recebido",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"INVALID_OR_EXPIRED_TOKEN\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "E-mail confirmado"),
        ApiResponse(responseCode = "400", description = "Token inválido ou expirado"),
        ApiResponse(responseCode = "422", description = "Corpo inválido"),
    )
    fun confirm(@Valid @RequestBody request: TokenRequest): ResponseEntity<Void> {
        service.confirmVerification(request.token)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/reset/request")
    @Operation(
        operationId = "requestPasswordReset",
        summary = "Envia o e-mail de redefinição de senha",
        description = "Responde 202 mesmo quando o e-mail não existe ou não tem senha local, para não revelar contas.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"VALIDATION_FAILED\", \"MALFORMED_REQUEST\", \"EMAIL_NOT_CONFIGURED\", \"EMAIL_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "202", description = "Pedido aceito"),
        ApiResponse(responseCode = "422", description = "Corpo inválido"),
        ApiResponse(responseCode = "503", description = "Envio de e-mail não configurado ou indisponível"),
    )
    fun requestReset(@Valid @RequestBody request: EmailRequest): ResponseEntity<Map<String, String>> {
        service.requestReset(request.email)
        return ResponseEntity.accepted().body(mapOf("status" to "REQUEST_ACCEPTED"))
    }

    @PostMapping("/password/reset/confirm")
    @Operation(
        operationId = "confirmPasswordReset",
        summary = "Redefine a senha com o token recebido",
        description = "Revoga todas as sessões da conta.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"MALFORMED_REQUEST\", \"PASSWORD_POLICY\", \"INVALID_OR_EXPIRED_TOKEN\", \"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Senha redefinida"),
        ApiResponse(responseCode = "400", description = "Token inválido ou expirado, ou senha fora da política"),
        ApiResponse(responseCode = "422", description = "Corpo ilegível"),
    )
    fun reset(@RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        service.reset(request.token, request.password)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/migrate")
    @Operation(
        operationId = "migrateFirebasePassword",
        summary = "Cria a senha local de uma conta que entrava pelo Firebase com e-mail e senha",
        description = "Exige um ID token do Firebase do provedor password, com e-mail verificado e vinculado à conta autenticada. Revoga todas as sessões da conta.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"MALFORMED_REQUEST\", \"UNAUTHENTICATED\", \"PASSWORD_POLICY\", \"IDENTITY_PROOF_REQUIRED\", \"MIGRATION_NOT_ALLOWED\", \"INVALID_FIREBASE_TOKEN\", \"ACCOUNT_UNAVAILABLE\", \"FIREBASE_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Senha local criada"),
        ApiResponse(responseCode = "400", description = "Senha fora da política, prova de identidade ausente ou migração não permitida"),
        ApiResponse(responseCode = "401", description = "Não autenticado ou ID token do Firebase inválido"),
        ApiResponse(responseCode = "403", description = "Conta indisponível"),
        ApiResponse(responseCode = "422", description = "Corpo ilegível"),
        ApiResponse(responseCode = "503", description = "Firebase indisponível"),
    )
    fun migrate(@AuthenticationPrincipal principal: AuthUser, @RequestBody request: MigratePasswordRequest): ResponseEntity<Void> {
        migration.migrate(principal.id, request.idToken, request.password)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/change")
    @Operation(
        operationId = "changePassword",
        summary = "Troca a senha do usuário autenticado",
        description = "Confere a senha atual e revoga todas as sessões da conta.",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"MALFORMED_REQUEST\", \"UNAUTHENTICATED\", \"INVALID_CREDENTIALS\", \"PASSWORD_POLICY\", \"LOCAL_CREDENTIAL_REQUIRED\", \"ACCOUNT_UNAVAILABLE\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Senha trocada"),
        ApiResponse(responseCode = "400", description = "Senha fora da política ou conta sem senha local"),
        ApiResponse(responseCode = "401", description = "Não autenticado ou senha atual incorreta"),
        ApiResponse(responseCode = "403", description = "Conta indisponível"),
        ApiResponse(responseCode = "422", description = "Corpo ilegível"),
    )
    fun change(@AuthenticationPrincipal principal: AuthUser, @RequestBody request: ChangePasswordRequest): ResponseEntity<Void> {
        service.change(principal.id, request.currentPassword, request.password)
        return ResponseEntity.noContent().build()
    }
}
