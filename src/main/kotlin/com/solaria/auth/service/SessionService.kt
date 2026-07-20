package com.solaria.auth.service

import com.solaria.auth.entity.Session
import com.solaria.auth.entity.UserAccount

interface SessionService {
    fun create(user: UserAccount, authenticationMethods: Array<String>, ip: String? = null, userAgent: String? = null, device: String? = null): Session
    fun revoke(session: Session, reason: String)
    fun revokeAllFor(user: UserAccount)
}
