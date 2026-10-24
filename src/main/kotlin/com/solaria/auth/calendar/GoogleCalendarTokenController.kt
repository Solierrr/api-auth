package com.solaria.auth.calendar

import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class GoogleCalendarTokenRequest(
    @field:NotBlank val accessToken: String?,
    @field:NotBlank val refreshToken: String?
)

@Hidden
@RestController
@RequestMapping("/internal/technicians/{technicianId}/google-calendar-token")
class GoogleCalendarTokenController(private val service: GoogleCalendarTokenService) {
    @GetMapping
    fun get(@PathVariable technicianId: String): ResponseEntity<GoogleCalendarTokens> =
        ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.connected(technicianId))

    @PutMapping
    fun save(@PathVariable technicianId: String, @Valid @RequestBody request: GoogleCalendarTokenRequest): ResponseEntity<Void> {
        service.save(technicianId, requireNotNull(request.accessToken), requireNotNull(request.refreshToken))
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping
    fun disconnect(@PathVariable technicianId: String): ResponseEntity<Void> {
        service.disconnect(technicianId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/revoke")
    fun revoke(@PathVariable technicianId: String): ResponseEntity<Void> {
        service.revoke(technicianId)
        return ResponseEntity.noContent().build()
    }
}
