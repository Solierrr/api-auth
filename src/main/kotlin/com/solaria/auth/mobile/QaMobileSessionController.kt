package com.solaria.auth.mobile

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class QaMobileSessionController(private val service: QaMobileSessionService) {
    @PostMapping("/auth/mobile/qa/session")
    fun login(@RequestHeader(value = "X-QA-Device-Token", required = false) deviceToken: String?, servlet: HttpServletRequest) =
        ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").header("Referrer-Policy", "no-referrer")
            .body(service.login(deviceToken, servlet.remoteAddr, servlet.getHeader("User-Agent")))
}
