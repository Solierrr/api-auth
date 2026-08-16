package com.solaria.auth.security.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Collections

/**
 * configuração das chaves RSA que o sistema usa para criar e validar JWTs.
 */
@Configuration
class JwtKeysConfig {
    @Bean
    fun jwtKeySet(properties: JwtProperties): JWKSet {

        // Instancia um KeyStore (armazenamento de chaves) do tipo PKCS12
        val keyStore = KeyStore.getInstance("PKCS12")

        // carrega o keystore
        FileInputStream(properties.keystorePath).use { input ->
            keyStore.load(input, properties.keystorePassword.toCharArray())
        }

        // 1. itera sobre todos os aliases(kids) no keystore
        // 2. extrai a chave publica do alias
        // 3. extrai a chave privada do alias
        // 4. transforma as chaves em uma RSAKey (representação de chave RSA no padrão JWK)
        val keys = Collections.list(keyStore.aliases()).map { alias ->

            val publicKey = keyStore.getCertificate(alias).publicKey as RSAPublicKey

            val privateKey = keyStore.getKey(alias, properties.keystorePassword.toCharArray()) as RSAPrivateKey

            RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(alias)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build()
        }

        // teste de falha, se o kid ativo não existir lança exceção
        require(keys.any { it.keyID == properties.activeKid }) {
            "JWT_ACTIVE_KID '${properties.activeKid}' has no matching entry (alias) in the keystore at " +
                "'${properties.keystorePath}'. Refusing to start: an active kid that cannot sign tokens is a " +
                "boot-time configuration error, not a first-request failure."
        }

        // Devolve o conjunto completo de chaves privadas e públicas
        return JWKSet(keys)
    }

    // Bean separado para adaptar o JWKSet em JWKSource, que utiliza o JWKSet como fonte de chaves
    @Bean
    fun jwkSource(jwtKeySet: JWKSet): JWKSource<SecurityContext> = ImmutableJWKSet(jwtKeySet)
}
