package com.solaria.auth.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.solaria.auth.security.config.JwtProperties
import com.solaria.auth.entity.UserAccount
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

/**
 * Serviço central de emissão/validação de access tokens JWT de usuário,
 * assinados com RS256 (par de chaves assimétrico) via Nimbus JOSE+JWT.
 */
@Service
class JwtService(
    private val properties: JwtProperties,
    jwkSource: JWKSource<SecurityContext>
) {
    // Encoder Nimbus -> assina/cria tokens novos escolhendo a chave pelo "kid" declarado no header
    private val encoder: JwtEncoder = NimbusJwtEncoder(jwkSource)

    // Decodifica e valida tokens JWT usando RS256 e o JWKSource
    private val decoder: JwtDecoder = run {

        // DefaultJWTProcessor criado manualmente com o seletor de chave kid para RS256
        // permite verificar tokens assinados com qualquer kid presente no JWKSet, não só o kid ativo atual
        val processor = DefaultJWTProcessor<SecurityContext>().apply {
            // seta o algoritmo usado e delega jwkSource para procurar a chave pública da assinatura
            setJWSKeySelector(JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource))
        }
        // transforma o processor em um NimbusJwtDecoder, tipo consumido pelo resto do Spring Security.
        NimbusJwtDecoder(processor).apply {

            // validação adicional de issuer e expiration time -> falha de qualquer -> token rejeitado
            setJwtValidator(
                DelegatingOAuth2TokenValidator(
                    JwtValidators.createDefaultWithIssuer(properties.issuer),
                    AccessTokenTypeValidator()
                )
            )
        }
    }

    // Ponto de entrada público para emitir um novo access token
    fun generateAccessToken(user: UserAccount, sessionId: UUID, authenticationMethods: Array<String>): String =
        generateToken(user, sessionId, authenticationMethods)

    // decodifica e valida um token extraindo duas claims userId e sessionId
    fun extractAccessTokenIdentity(token: String): AccessTokenIdentity = decoder.decode(token).let { jwt ->
        AccessTokenIdentity(
            userId = UUID.fromString(requireNotNull(jwt.subject) { "JWT subject is required" }),
            sessionId = UUID.fromString(requireNotNull(jwt.getClaimAsString("sid")) { "JWT session is required" })
        )
    }

    // Monta e assina um access token novo
    private fun generateToken(user: UserAccount, sessionId: UUID, authenticationMethods: Array<String>): String {
        // Instante de emissão do token
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            // "iss": identifica este serviço como emissor
            .issuer(properties.issuer)
            // "sub": id do usuário dono do token, como String (UUID).
            .subject(requireNotNull(user.id).toString())
            // "jti": id único deste token específico
            .id(UUID.randomUUID().toString())
            // "iat": instante de emissão.
            .issuedAt(now)
            // "exp": instante de expiração, calculado a partir do TTL configurado.
            .expiresAt(now.plus(properties.accessTokenTtl))
            // Claim customizado com o e-mail do usuário
            .claim("email", user.primaryEmail)
            // "sid": id da sessão associada a este token
            .claim("sid", sessionId.toString())
            // "amr" (authentication methods reference): quais fatores de autenticação foram usados para emitir este token
            .claim("amr", authenticationMethods.toList())
            // Claim customizado que marca este token como um access token
            .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
            .build()

        // Header assinado com RS256 e com o "kid" da chave ativa para assinatura
        val header = JwsHeader.with(SignatureAlgorithm.RS256)
            .keyId(properties.activeKid)
            .build()

        // Assina header+claims via o encoder Nimbus e devolve o token
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    private companion object {
        const val TOKEN_TYPE_CLAIM = "token_type"
        const val ACCESS_TOKEN_TYPE = "access"
    }
}

// Identidade mínima extraída de um access token já validado
data class AccessTokenIdentity(val userId: UUID, val sessionId: UUID)
