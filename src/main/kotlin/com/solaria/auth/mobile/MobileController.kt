package com.solaria.auth.mobile

import com.solaria.auth.security.AuthUser
import com.solaria.auth.exception.handler.ProblemDetailFactory
import com.solaria.auth.observability.HttpObservationErrors
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory

data class MobileConfirmation(val confirmed: Boolean)
data class MobileCallback(val callbackUrl: String)

@RestController
@RequestMapping("/auth/mobile")
class MobileController(private val service: MobileAuthorizationService) {
    @PostMapping("/transactions")
    fun start(@RequestBody request: StartMobile) = privateResponse(service.start(request))

    @GetMapping("/transactions/{id}")
    fun inspect(@PathVariable id: String, @AuthenticationPrincipal principal: AuthUser) =
        privateResponse(service.inspect(id, principal))

    @PostMapping("/transactions/{id}/authorize")
    fun authorize(@PathVariable id: String, @AuthenticationPrincipal principal: AuthUser,
        @RequestBody request: MobileConfirmation) =
        privateResponse(MobileCallback(service.authorize(id, principal, request.confirmed)))

    @DeleteMapping("/transactions/{id}")
    fun cancel(@PathVariable id: String, @AuthenticationPrincipal principal: AuthUser): ResponseEntity<Void> {
        service.cancel(id, principal)
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build()
    }

    @PostMapping("/token")
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
