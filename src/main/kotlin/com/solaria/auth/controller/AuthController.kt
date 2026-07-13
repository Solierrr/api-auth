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
@RequestMapping("/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register/company")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerCompany(@Valid @RequestBody request: RegisterCompanyRequest): AuthResponse {
        return authService.registerCompany(request)
    }

    @PostMapping("/register/professional/contractor")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerContractor(@Valid @RequestBody request: RegisterContractorRequest): AuthResponse {
        return authService.registerContractor(request)
    }

    @PostMapping("/register/professional/freelancer")
    @ResponseStatus(HttpStatus.CREATED)
    fun registerFreelancer(@Valid @RequestBody request: RegisterFreelancerRequest): AuthResponse {
        return authService.registerFreelancer(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(request)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthResponse {
        return authService.refresh(request)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@AuthenticationPrincipal principal: AuthUser) {
        authService.logout(principal.id)
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest) {
        authService.forgotPassword(request)
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest) {
        authService.resetPassword(request)
    }

}