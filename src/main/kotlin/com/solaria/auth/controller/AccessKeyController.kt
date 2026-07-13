package com.solaria.auth.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/access-keys")
class AccessKeyController(private val accessKeyService: AccessKeyService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal principal: AuthUser,
        @Valid @RequestBody request: CreateAccessKeyRequest): AccessKeyResponse {
        return accessKeyService.create(principal.companyId, principal.id, request)
    }

    @GetMapping
    fun findAll(@AuthenticationPrincipal principal: AuthUser): List<AccessKeyResponse> {
        return accessKeyService.findAll(principal.companyId)
    }

    @GetMapping("/{id}")
    fun findById(@AuthenticationPrincipal principal: AuthUser, @PathVariable id: Long): AccessKeyResponse {
        return accessKeyService.findById(principal.companyId, id)
    }

    @PatchMapping("/{id}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(@AuthenticationPrincipal principal: AuthUser, @PathVariable id: Long) {
        accessKeyService.revoke(principal.companyId, id)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal principal: AuthUser, @PathVariable id: Long) {
        accessKeyService.delete(principal.companyId, id)
    }
}