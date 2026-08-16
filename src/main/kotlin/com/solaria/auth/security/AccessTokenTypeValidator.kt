package com.solaria.auth.security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Valida se o token informado é uma acesses token
 */

// implementa OAuth2TokenValidator<Jwt> -> plug no decoder de JwtService via DelegatingOAuth2TokenValidator
// -> roda em toda decodificação de token
class AccessTokenTypeValidator : OAuth2TokenValidator<Jwt> {

    override fun validate(token: Jwt): OAuth2TokenValidatorResult =
        if (token.getClaimAsString(TOKEN_TYPE_CLAIM) == ACCESS_TOKEN_TYPE) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(
                OAuth2Error("invalid_token", "JWT não é um access token válido", null)
            )
        }

    private companion object {
        const val TOKEN_TYPE_CLAIM = "token_type"
        const val ACCESS_TOKEN_TYPE = "access"
    }
}
