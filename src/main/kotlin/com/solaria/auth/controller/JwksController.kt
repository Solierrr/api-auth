package com.solaria.auth.controller

import com.nimbusds.jose.jwk.JWKSet
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.extensions.Extension
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint responsável por provisionar as chaves públicas
 */
@RestController
@Tag(name = "Chaves públicas", description = "Chaves públicas (JWKS) para os outros serviços validarem os JWTs emitidos")
class JwksController(
    private val jwtKeySet: JWKSet
) {
    @GetMapping("/.well-known/jwks.json")
    @Operation(
        operationId = "getJwks",
        summary = "Retorna o conjunto de chaves públicas usadas para validar os JWTs",
        extensions = [Extension(properties = [ExtensionProperty(name = "x-error-codes", parseValue = true, value = "[\"INTERNAL_ERROR\"]")])],
    )
    @SecurityRequirements
    @ApiResponses(ApiResponse(responseCode = "200", description = "Conjunto de chaves públicas"))
    fun jwks(): Map<String, Any> = jwtKeySet.toPublicJWKSet().toJSONObject()
}
