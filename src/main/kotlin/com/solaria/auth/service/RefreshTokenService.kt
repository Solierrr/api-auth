package com.solaria.auth.service

import com.solaria.auth.entity.RefreshToken
import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount

data class IssuedRefreshToken(val entity: RefreshToken, val value: String)
class RefreshTokenReuseException : RuntimeException("Refresh token reuse detected")

interface RefreshTokenService {
    fun issue(session: Session): IssuedRefreshToken
    fun rotate(value: String): IssuedRefreshToken
    fun revokeAllFor(user: UserAccount)
}
