package com.solaria.auth.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import com.solaria.auth.config.JwtProperties
import com.solaria.auth.entity.UserAccount
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    private val properties: JwtProperties
) {
    private val secretKey: SecretKey by lazy {
        val keyBytes = Base64.getDecoder().decode(properties.secret)
        require(keyBytes.size >= MINIMUM_KEY_SIZE_BYTES) {
            "JWT_SECRET must contain at least 32 bytes"
        }
        SecretKeySpec(keyBytes, HMAC_ALGORITHM)
    }

    private val encoder: JwtEncoder by lazy {
        NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey))
    }

    private val decoder: JwtDecoder by lazy {
        NimbusJwtDecoder.withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .also { it.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer)) }
    }

    fun generateAccessToken(user: UserAccount, sessionId: UUID, authenticationMethods: Array<String>): String =
        generateToken(user, sessionId, authenticationMethods)

    fun extractAccessTokenIdentity(token: String): AccessTokenIdentity = decoder.decode(token).let { jwt ->
        require(jwt.getClaimAsString(TOKEN_TYPE_CLAIM) == ACCESS_TOKEN_TYPE) {
            "JWT is not an access token"
        }
        AccessTokenIdentity(
            userId = UUID.fromString(requireNotNull(jwt.subject) { "JWT subject is required" }),
            sessionId = UUID.fromString(requireNotNull(jwt.getClaimAsString("sid")) { "JWT session is required" })
        )
    }

    private fun generateToken(user: UserAccount, sessionId: UUID, authenticationMethods: Array<String>): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(requireNotNull(user.id).toString())
            .id(UUID.randomUUID().toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.accessTokenTtl))
            .claim("email", user.primaryEmail)
            .claim("sid", sessionId.toString())
            .claim("amr", authenticationMethods.toList())
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .build()

        return encoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).tokenValue
    }

    private companion object {
        const val HMAC_ALGORITHM = "HmacSHA256"
        const val MINIMUM_KEY_SIZE_BYTES = 32
        const val TOKEN_TYPE_CLAIM = "token_type"
        const val ACCESS_TOKEN_TYPE = "access"
    }
}

data class AccessTokenIdentity(val userId: UUID, val sessionId: UUID)
