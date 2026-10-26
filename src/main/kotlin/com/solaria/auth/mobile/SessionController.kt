package com.solaria.auth.mobile

import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.repository.SessionRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.AuthUser
import com.solaria.auth.service.SessionService
import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@RestController
@Transactional
@Tag(name = "Sessões", description = "Usuário autenticado e encerramento de sessões")
class SessionController(
    private val sessions: SessionRepository,
    private val users: UserAccountRepository,
    private val service: SessionService,
    @Value("\${app.mobile.service-token:}") private val serviceToken: String
) {
    @GetMapping("/auth/me")
    @Operation(
        operationId = "getCurrentUser",
        summary = "Retorna o usuário e a sessão autenticados",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Usuário e sessão"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
    )
    fun me(@AuthenticationPrincipal principal: AuthUser): Map<String, Any?> {
        val user = users.findById(principal.id).orElseThrow()
        val session = sessions.findById(requireNotNull(principal.sessionId)).orElseThrow()
        return mapOf("userId" to user.id, "email" to user.primaryEmail,
            "emailVerified" to (user.emailVerifiedAt != null), "sessionId" to session.id,
            "clientId" to session.clientId, "context" to session.context)
    }

    @PostMapping("/auth/sessions/current/logout")
    @Operation(
        operationId = "logoutCurrentSession",
        summary = "Encerra só a sessão atual",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"UNAUTHENTICATED\", \"INVALID_SESSION\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Sessão encerrada"),
        ApiResponse(responseCode = "401", description = "Não autenticado ou sessão de outro usuário"),
    )
    fun logout(@AuthenticationPrincipal principal: AuthUser): ResponseEntity<Void> {
        val session = sessions.findById(requireNotNull(principal.sessionId)).orElseThrow()
        if (session.user?.id != principal.id) throw MobileFlowException("INVALID_SESSION", HttpStatus.UNAUTHORIZED)
        service.revoke(session, "SESSION_LOGOUT")
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/auth/sessions/logout-all")
    @Operation(
        operationId = "logoutAllSessions",
        summary = "Encerra todas as sessões do usuário",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"UNAUTHENTICATED\", \"INTERNAL_ERROR\"]")])],
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Sessões encerradas"),
        ApiResponse(responseCode = "401", description = "Não autenticado"),
    )
    fun logoutAll(@AuthenticationPrincipal principal: AuthUser): ResponseEntity<Void> {
        service.revokeAllFor(users.findById(principal.id).orElseThrow())
        return ResponseEntity.noContent().build()
    }

    @Hidden
    @GetMapping("/internal/sessions/{id}")
    fun active(@PathVariable id: UUID, @RequestHeader(value = "Authorization", defaultValue = "") authorization: String): Map<String, Any?> {
        if (serviceToken.length < 32) throw MobileFlowException("SERVICE_NOT_CONFIGURED", HttpStatus.SERVICE_UNAVAILABLE)
        if (!MessageDigest.isEqual(authorization.toByteArray(), "Bearer $serviceToken".toByteArray())) {
            throw MobileFlowException("INVALID_SERVICE_CREDENTIAL", HttpStatus.UNAUTHORIZED)
        }
        val session = sessions.findById(id).orElse(null)
        val user = session?.user
        val active = session != null && user != null && session.revokedAt == null
            && session.expiresAt.isAfter(Instant.now()) && user.status == AccountStatus.ACTIVE
            && user.lockedUntil?.isAfter(Instant.now()) != true
        return mapOf("active" to active, "authId" to user?.id, "clientId" to session?.clientId,
            "context" to session?.context)
    }
}
