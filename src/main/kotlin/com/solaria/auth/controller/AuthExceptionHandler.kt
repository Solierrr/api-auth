package com.solaria.auth.controller

import com.solaria.auth.exception.handler.ProblemDetailFactory
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
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.ErrorResponse as SpringErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Centraliza o tratamento de exceções da API de auth e devolve todo erro como [ProblemDetail]
 *
 * <p>
 * Cobre regras de negócio, validação, JSON malformado e erros inesperados -> web e mobile recebem sempre o mesmo formato
 * </p>
 *
 * <p>
 * Toda exceção tratada é marcada como erro no tracing ([HttpObservationErrors.mark]: span `ERROR` + tag `exception`
 * em `http.server.requests`) e gera log correlacionado ao trace: `WARN` para 4xx, `ERROR` com stack trace para 5xx
 * </p>
 *
 * <p>Corpo montado por [ProblemDetailFactory]</p>
 */
@RestControllerAdvice
class AuthExceptionHandler(
    private val request: HttpServletRequest,
    private val factory: ProblemDetailFactory,
) {
    private val log = LoggerFactory.getLogger(AuthExceptionHandler::class.java)

    // Marca a observação da request como erro e registra um log correlacionado ao trace
    private fun recordError(exception: Throwable, status: HttpStatus, errorCode: String) {
        HttpObservationErrors.mark(request, exception)
        if (status.is5xxServerError) {
            log.atError().addKeyValue("errorCode", errorCode).log("Falha {} em {} {}", status.value(), request.method, request.requestURI, exception)
        } else {
            log.atWarn().addKeyValue("errorCode", errorCode).log("Erro {} em {} {}: {}", status.value(), request.method, request.requestURI, exception.message)
        }
    }

    @ExceptionHandler(AuthenticationException::class)
    fun invalidCredentials(exception: AuthenticationException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS")
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is invalid")
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDenied(exception: AccessDeniedException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.FORBIDDEN, "FORBIDDEN")
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Acesso negado: você não tem permissão para acessar este recurso.")
    }

    @ExceptionHandler(EmailAlreadyRegisteredException::class)
    fun emailAlreadyRegistered(exception: EmailAlreadyRegisteredException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED")
        return error(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered")
    }

    @ExceptionHandler(InvalidRefreshTokenException::class)
    fun invalidRefreshToken(exception: InvalidRefreshTokenException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN")
        return error(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid")
    }

    @ExceptionHandler(RefreshTokenReuseException::class)
    fun refreshTokenReuse(exception: RefreshTokenReuseException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED")
        return error(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Refresh token reuse was detected")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validationError(exception: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED")
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_FAILED", "Erro de validação nos campos da requisição.",
            mapOf("errors" to exception.bindingResult.fieldErrors.map {
                mapOf("field" to it.field, "errorCode" to "VALIDATION_FAILED", "detail" to (it.defaultMessage ?: "Valor inválido."))
            }))
    }

    // Corpo de request malformado/ilegivel
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun malformedRequest(exception: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNPROCESSABLE_CONTENT, "MALFORMED_REQUEST")
        return error(HttpStatus.UNPROCESSABLE_CONTENT, "MALFORMED_REQUEST", "Corpo da requisição malformado ou ilegível.")
    }

    @ExceptionHandler(AccountLinkRequiredException::class)
    fun accountLinkRequired(exception: AccountLinkRequiredException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.CONFLICT, "ACCOUNT_LINK_REQUIRED")
        return error(HttpStatus.CONFLICT, "ACCOUNT_LINK_REQUIRED",
            "Authenticate with the existing account before linking Firebase", mapOf("email" to exception.email))
    }

    @ExceptionHandler(AccountUnavailableException::class)
    fun accountUnavailable(exception: AccountUnavailableException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE")
        return error(HttpStatus.FORBIDDEN, "ACCOUNT_UNAVAILABLE", "User account is unavailable")
    }

    @ExceptionHandler(InvalidAccountLinkCredentialsException::class)
    fun invalidAccountLinkCredentials(exception: InvalidAccountLinkCredentialsException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNAUTHORIZED, "INVALID_ACCOUNT_LINK_CREDENTIALS")
        return error(HttpStatus.UNAUTHORIZED, "INVALID_ACCOUNT_LINK_CREDENTIALS", "Local account credentials are invalid")
    }

    @ExceptionHandler(FirebaseAccountLinkMismatchException::class)
    fun firebaseAccountLinkMismatch(exception: FirebaseAccountLinkMismatchException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.CONFLICT, "FIREBASE_ACCOUNT_LINK_MISMATCH")
        return error(HttpStatus.CONFLICT, "FIREBASE_ACCOUNT_LINK_MISMATCH", "Firebase email does not match the account being linked")
    }

    @ExceptionHandler(FederatedIdentityConflictException::class)
    fun federatedIdentityConflict(exception: FederatedIdentityConflictException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.CONFLICT, "FEDERATED_IDENTITY_CONFLICT")
        return error(HttpStatus.CONFLICT, "FEDERATED_IDENTITY_CONFLICT", "Firebase identity is already linked to another account")
    }

    @ExceptionHandler(InvalidFirebaseTokenException::class)
    fun invalidFirebaseToken(exception: InvalidFirebaseTokenException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNAUTHORIZED, "INVALID_FIREBASE_TOKEN")
        return error(HttpStatus.UNAUTHORIZED, "INVALID_FIREBASE_TOKEN", "Firebase ID token is invalid")
    }

    @ExceptionHandler(VerifiedFirebaseEmailRequiredException::class)
    fun verifiedEmailRequired(exception: VerifiedFirebaseEmailRequiredException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.UNPROCESSABLE_ENTITY, "VERIFIED_FIREBASE_EMAIL_REQUIRED")
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VERIFIED_FIREBASE_EMAIL_REQUIRED", "A verified Firebase email is required")
    }

    @ExceptionHandler(FirebaseUnavailableException::class)
    fun firebaseUnavailable(exception: FirebaseUnavailableException): ResponseEntity<ProblemDetail> {
        recordError(exception, HttpStatus.SERVICE_UNAVAILABLE, "FIREBASE_UNAVAILABLE")
        return error(HttpStatus.SERVICE_UNAVAILABLE, "FIREBASE_UNAVAILABLE", "Firebase authentication is unavailable")
    }

    /**
     * Catch-all
     * marca o span e loga com stack trace
     */
    @ExceptionHandler(Exception::class)
    fun unhandled(exception: Exception): ResponseEntity<ProblemDetail> {
        if (exception is SpringErrorResponse) {
            val received = HttpStatus.valueOf(exception.statusCode.value())
            val status = if (received == HttpStatus.BAD_REQUEST) HttpStatus.UNPROCESSABLE_CONTENT else received
            val code = genericErrorCode(status)
            recordError(exception, status, code)
            return error(status, code, "A requisição não pôde ser processada.")
        }
        recordError(exception, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR")
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            "Ocorreu um erro interno inesperado. Se persistir, contate o suporte.")
    }

    private fun genericErrorCode(status: HttpStatus): String = when (status) {
        HttpStatus.UNPROCESSABLE_CONTENT -> "VALIDATION_FAILED"
        HttpStatus.UNAUTHORIZED -> "UNAUTHENTICATED"
        HttpStatus.FORBIDDEN -> "FORBIDDEN"
        HttpStatus.NOT_FOUND -> "NOT_FOUND"
        HttpStatus.METHOD_NOT_ALLOWED -> "METHOD_NOT_ALLOWED"
        HttpStatus.NOT_ACCEPTABLE -> "NOT_ACCEPTABLE"
        HttpStatus.CONFLICT -> "CONFLICT"
        HttpStatus.UNSUPPORTED_MEDIA_TYPE -> "UNSUPPORTED_MEDIA_TYPE"
        HttpStatus.TOO_MANY_REQUESTS -> "RATE_LIMITED"
        HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE -> "DEPENDENCY_UNAVAILABLE"
        else -> if (status.is4xxClientError) "VALIDATION_FAILED" else "INTERNAL_ERROR"
    }

    private fun error(status: HttpStatus, code: String, message: String,
        properties: Map<String, Any> = emptyMap()): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(status).body(factory.create(status, message, code,
            properties + mapOf("code" to code, "message" to message), request))
}
