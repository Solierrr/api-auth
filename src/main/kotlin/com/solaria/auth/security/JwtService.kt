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
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Base64
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
    }

    fun generateAccessToken(user: UserAccount): String =
        generateToken(user, properties.accessTokenTtl, ACCESS_TOKEN_TYPE)

    fun generateRefreshToken(user: UserAccount): String =
        generateToken(user, properties.refreshTokenTtl, REFRESH_TOKEN_TYPE)

    fun extractAccessTokenUserId(token: String): String = decoder.decode(token).let { jwt ->
        require(jwt.getClaimAsString(TOKEN_TYPE_CLAIM) == ACCESS_TOKEN_TYPE) {
            "JWT is not an access token"
        }
        jwt.subject
    }

    private fun generateToken(user: UserAccount, ttl: java.time.Duration, tokenType: String): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(requireNotNull(user.id).toString())
            .issuedAt(now)
            .expiresAt(now.plus(ttl))
            .claim("email", user.email)
            .claim(TOKEN_TYPE_CLAIM, tokenType)
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
        const val REFRESH_TOKEN_TYPE = "refresh"
    }
}
