package com.solaria.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.security.jwt")
data class JwtProperties(
    val secret: String,
    val issuer: String,
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration
)
