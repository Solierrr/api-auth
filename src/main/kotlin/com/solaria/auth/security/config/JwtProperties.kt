package com.solaria.auth.security.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

// Liga esta classe ao prefixo "app.security.jwt" do application.properties
@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    val issuer: String,
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration,
    val keystorePath: String,
    val keystorePassword: String,
    val activeKid: String
)
