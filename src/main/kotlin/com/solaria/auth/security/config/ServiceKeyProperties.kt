package com.solaria.auth.security.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.service-keys")
class ServiceKeyProperties : LinkedHashMap<String, String>() {
    @PostConstruct
    fun validate() {
        forEach { (service, key) ->
            check(key.isBlank() || key.length >= MIN_KEY_LENGTH) {
                "app.service-keys.$service must have at least $MIN_KEY_LENGTH characters"
            }
        }
    }

    private companion object {
        const val MIN_KEY_LENGTH = 32
    }
}
