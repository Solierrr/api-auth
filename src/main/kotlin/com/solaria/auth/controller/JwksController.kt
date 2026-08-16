package com.solaria.auth.controller

import com.nimbusds.jose.jwk.JWKSet
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint responsável por provisionar as chaves públicas
 */
@RestController
class JwksController(
    private val jwtKeySet: JWKSet
) {
    @GetMapping("/.well-known/jwks.json")
    fun jwks(): Map<String, Any> = jwtKeySet.toPublicJWKSet().toJSONObject()
}
