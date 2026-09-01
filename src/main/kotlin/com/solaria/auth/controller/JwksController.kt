package com.solaria.auth.controller

import com.nimbusds.jose.jwk.JWKSet
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint responsável por provisionar as chaves públicas
 */
@RestController
@Tag(name = "JWKS", description = "Endpoint de provisionamento das chaves públicas")
class JwksController(
    private val jwtKeySet: JWKSet
) {
    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "Retorna o conjunto de chaves públicas usadas para validar os JWTs")
    fun jwks(): Map<String, Any> = jwtKeySet.toPublicJWKSet().toJSONObject()
}
