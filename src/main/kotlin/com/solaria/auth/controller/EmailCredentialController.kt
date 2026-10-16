package com.solaria.auth.controller

import com.solaria.auth.security.AuthUser
import com.solaria.auth.service.EmailCredentialService
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

data class EmailRequest(@field:Email @field:NotBlank @field:Size(max = 254) val email: String)
data class TokenRequest(@field:Size(min = 43, max = 43) val token: String)
data class ResetPasswordRequest(val token: String, val password: String)
data class MigratePasswordRequest(val idToken: String, val password: String)
 data class ChangePasswordRequest(val currentPassword: String, val password: String)

@RestController
@RequestMapping("/auth")
class EmailCredentialController(private val service: EmailCredentialService,
    private val migration: com.solaria.auth.service.FirebasePasswordMigrationService) {
    @PostMapping("/email/verification/request")
    fun requestVerification(@AuthenticationPrincipal principal: AuthUser): ResponseEntity<Map<String, String>> {
        service.requestVerification(principal.id)
        return ResponseEntity.accepted().body(mapOf("status" to "REQUEST_ACCEPTED"))
    }

    @PostMapping("/email/verification/confirm")
    fun confirm(@Valid @RequestBody request: TokenRequest): ResponseEntity<Void> {
        service.confirmVerification(request.token)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/reset/request")
    fun requestReset(@Valid @RequestBody request: EmailRequest): ResponseEntity<Map<String, String>> {
        service.requestReset(request.email)
        return ResponseEntity.accepted().body(mapOf("status" to "REQUEST_ACCEPTED"))
    }

    @PostMapping("/password/reset/confirm")
    fun reset(@RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        service.reset(request.token, request.password)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/migrate")
    fun migrate(@AuthenticationPrincipal principal: AuthUser, @RequestBody request: MigratePasswordRequest): ResponseEntity<Void> {
        migration.migrate(principal.id, request.idToken, request.password)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/change")
    fun change(@AuthenticationPrincipal principal: AuthUser, @RequestBody request: ChangePasswordRequest): ResponseEntity<Void> {
        service.change(principal.id, request.currentPassword, request.password)
        return ResponseEntity.noContent().build()
    }
}
