package com.solaria.auth.controller

import com.solaria.auth.dto.ErrorResponse
import com.solaria.auth.dto.auth.response.AccountLinkRequiredResponse
import com.solaria.auth.observability.HttpObservationErrors
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
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.ErrorResponse as SpringErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Tratamento centralizado de exceções da API de auth
 *
 * Observabilidade -> toda exceção tratada é marcada como erro no tracing
 * ([HttpObservationErrors.mark] -> span `ERROR` + tag `exception` em `http.server.requests`
 * `WARN` para 4xx (só mensagem)
 * `ERROR` com stack para 5xx
 */
@RestControllerAdvice
class AuthExceptionHandler(
    private val request: HttpServletRequest,
) {
    private val log = LoggerFactory.getLogger(AuthExceptionHandler::class.java)

    // Marca a observação da request como erro e registra um log correlacionado ao trace
    private fun recordError(exception: Throwable, status: HttpStatus) {
        HttpObservationErrors.mark(request, exception)
        if (status.is5xxServerError) {
            log.error("Falha {} em {} {}", status.value(), request.method, request.requestURI, exception)
        } else {
            log.warn("Erro {} em {} {}: {}", status.value(), request.method, request.requestURI, exception.message)
        }
    }

    @ExceptionHandler(AuthenticationException::class)
    fun invalidCredentials(exception: AuthenticationException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.UNAUTHORIZED)
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is invalid")
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDenied(exception: AccessDeniedException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.FORBIDDEN)
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access is denied")
    }

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun emailAlreadyRegistered(exception: EmailAlreadyRegisteredException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.CONFLICT)
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered")
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun invalidRefreshToken(exception: InvalidRefreshTokenException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.UNAUTHORIZED)
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid")
    }

    @ExceptionHandler(RefreshTokenReuseException::class)
    fun refreshTokenReuse(exception: RefreshTokenReuseException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.UNAUTHORIZED)
        return error(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Refresh token reuse was detected")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validationError(exception: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.BAD_REQUEST)
        return ResponseEntity.badRequest().body(
            ErrorResponse(
                status = "VALIDATION_ERROR",
                message = "Request validation failed",
                errors = exception.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
            )
        )
    }

    // Corpo de request malformado/ilegivel
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedRequest(exception: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.BAD_REQUEST)
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is malformed or unreadable")
    }

    @ExceptionHandler(AccountLinkRequiredException::class)
    fun accountLinkRequired(exception: AccountLinkRequiredException): ResponseEntity<AccountLinkRequiredResponse> {
        recordError(exception, HttpStatus.CONFLICT)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(AccountLinkRequiredResponse(email = exception.email))
    }

    @ExceptionHandler(AccountUnavailableException::class)
    fun accountUnavailable(exception: AccountUnavailableException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.FORBIDDEN)
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "User account is unavailable")
    }

    @ExceptionHandler(InvalidAccountLinkCredentialsException::class)
    fun invalidAccountLinkCredentials(exception: InvalidAccountLinkCredentialsException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.UNAUTHORIZED)
        return error(HttpStatus.UNAUTHORIZED, "INVALID_ACCOUNT_LINK_CREDENTIALS", "Local account credentials are invalid")
    }

    @ExceptionHandler(FirebaseAccountLinkMismatchException::class)
    fun firebaseAccountLinkMismatch(exception: FirebaseAccountLinkMismatchException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.CONFLICT)
        return error(HttpStatus.CONFLICT, "FIREBASE_ACCOUNT_LINK_MISMATCH", "Firebase email does not match the account being linked")
    }

    @ExceptionHandler(FederatedIdentityConflictException::class)
    fun federatedIdentityConflict(exception: FederatedIdentityConflictException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.CONFLICT)
        return error(HttpStatus.CONFLICT, "FEDERATED_IDENTITY_CONFLICT", "Firebase identity is already linked to another account")
    }

    @ExceptionHandler(InvalidFirebaseTokenException::class)
    fun invalidFirebaseToken(exception: InvalidFirebaseTokenException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.UNAUTHORIZED)
        return error(HttpStatus.UNAUTHORIZED, "INVALID_FIREBASE_TOKEN", "Firebase ID token is invalid")
    }

    @ExceptionHandler(VerifiedFirebaseEmailRequiredException::class)
    fun verifiedEmailRequired(exception: VerifiedFirebaseEmailRequiredException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.UNPROCESSABLE_ENTITY)
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VERIFIED_FIREBASE_EMAIL_REQUIRED", "A verified Firebase email is required")
    }

    @ExceptionHandler(FirebaseUnavailableException::class)
    fun firebaseUnavailable(exception: FirebaseUnavailableException): ResponseEntity<ErrorResponse> {
        recordError(exception, HttpStatus.SERVICE_UNAVAILABLE)
        return error(HttpStatus.SERVICE_UNAVAILABLE, "FIREBASE_UNAVAILABLE", "Firebase authentication is unavailable")
    }

    /**
     * Catch-all
     * marca o span e loga com stack trace
     */
    @ExceptionHandler(Exception::class)
    fun unhandled(exception: Exception): ResponseEntity<ErrorResponse> {
        if (exception is SpringErrorResponse) {
            val status = HttpStatus.valueOf(exception.statusCode.value())
            recordError(exception, status)
            return error(status, "REQUEST_ERROR", "The request could not be processed")
        }
        recordError(exception, HttpStatus.INTERNAL_SERVER_ERROR)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected internal error occurred")
    }

    private fun error(status: HttpStatus, code: String, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(status = code, message = message, errors = null))
}
