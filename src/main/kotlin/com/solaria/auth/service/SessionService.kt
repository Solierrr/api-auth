package com.solaria.auth.service

import com.solaria.auth.entity.RefreshToken
import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount

interface SessionService {
    fun create(user: UserAccount, refreshToken: RefreshToken, ip: String? = null, userAgent: String? = null, device: String? = null): Session
    fun revokeAllFor(user: UserAccount)
}
