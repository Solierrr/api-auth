package com.solaria.auth.security.firebase

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security.firebase")
data class FirebaseProperties(
    val enabled: Boolean = false,
    val projectId: String = ""
)
