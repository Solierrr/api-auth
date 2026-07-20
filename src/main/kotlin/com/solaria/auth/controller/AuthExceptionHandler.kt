package com.solaria.auth.controller

import com.solaria.auth.dto.ErrorResponse
import com.solaria.auth.dto.auth.response.AccountLinkRequiredResponse
import com.solaria.auth.security.firebase.FirebaseUnavailableException
import com.solaria.auth.security.firebase.InvalidFirebaseTokenException
import com.solaria.auth.service.AccountLinkRequiredException
import com.solaria.auth.service.AccountUnavailableException
import com.solaria.auth.service.FederatedIdentityConflictException
import com.solaria.auth.service.FirebaseAccountLinkMismatchException
import com.solaria.auth.service.InvalidAccountLinkCredentialsException
import com.solaria.auth.service.VerifiedFirebaseEmailRequiredException
import com.solaria.auth.service.EmailAlreadyRegisteredException
import com.solaria.auth.service.InvalidRefreshTokenException
import com.solaria.auth.service.RefreshTokenReuseException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException

@RestControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(AuthenticationException::class)
    fun invalidCredentials(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is invalid")

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun emailAlreadyRegistered(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered")

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun invalidRefreshToken(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid")

    @ExceptionHandler(RefreshTokenReuseException::class)
    fun refreshTokenReuse(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Refresh token reuse was detected")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validationError(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest().body(
            ErrorResponse(
                status = "VALIDATION_ERROR",
                message = "Request validation failed",
                errors = exception.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
            )
        )

    @ExceptionHandler(AccountLinkRequiredException::class)
    fun accountLinkRequired(exception: AccountLinkRequiredException): ResponseEntity<AccountLinkRequiredResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(AccountLinkRequiredResponse(email = exception.email))

    @ExceptionHandler(AccountUnavailableException::class)
    fun accountUnavailable(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "User account is unavailable")

    @ExceptionHandler(InvalidAccountLinkCredentialsException::class)
    fun invalidAccountLinkCredentials(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNAUTHORIZED, "INVALID_ACCOUNT_LINK_CREDENTIALS", "Local account credentials are invalid")

    @ExceptionHandler(FirebaseAccountLinkMismatchException::class)
    fun firebaseAccountLinkMismatch(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.CONFLICT, "FIREBASE_ACCOUNT_LINK_MISMATCH", "Firebase email does not match the account being linked")

    @ExceptionHandler(FederatedIdentityConflictException::class)
    fun federatedIdentityConflict(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.CONFLICT, "FEDERATED_IDENTITY_CONFLICT", "Firebase identity is already linked to another account")

    @ExceptionHandler(InvalidFirebaseTokenException::class)
    fun invalidFirebaseToken(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNAUTHORIZED, "INVALID_FIREBASE_TOKEN", "Firebase ID token is invalid")

    @ExceptionHandler(VerifiedFirebaseEmailRequiredException::class)
    fun verifiedEmailRequired(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.UNPROCESSABLE_ENTITY, "VERIFIED_FIREBASE_EMAIL_REQUIRED", "A verified Firebase email is required")

    @ExceptionHandler(FirebaseUnavailableException::class)
    fun firebaseUnavailable(): ResponseEntity<ErrorResponse> =
        error(HttpStatus.SERVICE_UNAVAILABLE, "FIREBASE_UNAVAILABLE", "Firebase authentication is unavailable")

    private fun error(status: HttpStatus, code: String, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(status = code, message = message, errors = null))
}
