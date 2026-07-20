package com.solaria.auth.security

import java.security.MessageDigest

object TokenHashing {
    fun sha256(value: String): ByteArray = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
}
