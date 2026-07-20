package com.solaria.auth.service

import com.solaria.auth.entity.RefreshToken
import com.solaria.auth.entity.UserAccount

interface RefreshTokenService {
    fun create(user: UserAccount, value: String): RefreshToken
    fun validate(value: String): RefreshToken
    fun revoke(refreshToken: RefreshToken)
    fun revokeAllFor(user: UserAccount)
}
