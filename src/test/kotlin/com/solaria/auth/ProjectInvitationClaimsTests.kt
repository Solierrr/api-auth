package com.solaria.auth

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.SignedJWT
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.security.JwtService
import com.solaria.auth.security.config.JwtProperties
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class ProjectInvitationClaimsTests {
    @Test
    fun `access tokens report verified email using the account verification state`() {
        val key = RSAKeyGenerator(2048).keyID("project-test").generate()
        val properties = JwtProperties("solaria-test", Duration.ofMinutes(10), Duration.ofDays(1), "unused", "unused", "project-test")
        val service = JwtService(properties, ImmutableJWKSet<SecurityContext>(JWKSet(key)))
        val user = UserAccount(id = UUID.randomUUID(), primaryEmail = "participant@solaria.test")
        val unverified = SignedJWT.parse(service.generateAccessToken(user, UUID.randomUUID(), arrayOf("pwd")))
        assertEquals(false, unverified.jwtClaimsSet.getBooleanClaim("email_verified"))
        user.emailVerifiedAt = Instant.now()
        val verified = SignedJWT.parse(service.generateAccessToken(user, UUID.randomUUID(), arrayOf("pwd")))
        assertEquals(true, verified.jwtClaimsSet.getBooleanClaim("email_verified"))
        assertEquals(user.primaryEmail, verified.jwtClaimsSet.getStringClaim("email"))
    }
}
