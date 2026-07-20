package com.solaria.auth.service

import com.solaria.auth.entity.OneTimeToken
import com.solaria.auth.enums.OneTimeTokenType
import com.solaria.auth.entity.UserAccount
import java.time.Instant

interface TokenService {
    fun create(user: UserAccount, value: String, type: OneTimeTokenType, expiresAt: Instant): OneTimeToken
    fun consume(value: String, type: OneTimeTokenType): OneTimeToken
}
