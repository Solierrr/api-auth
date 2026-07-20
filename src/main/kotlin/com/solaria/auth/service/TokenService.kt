package com.solaria.auth.service

import com.solaria.auth.entity.Token
import com.solaria.auth.entity.UserAccount
import java.time.Instant

interface TokenService {
    fun create(user: UserAccount, value: String, type: String, expiresAt: Instant): Token
    fun consume(value: String, type: String): Token
}
