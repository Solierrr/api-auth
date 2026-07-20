package com.solaria.auth.service

import com.solaria.auth.entity.OAuthAccount
import com.solaria.auth.entity.UserAccount

interface OAuthService {
    fun link(user: UserAccount, provider: String, providerUserId: String): OAuthAccount
    fun findLinkedAccount(provider: String, providerUserId: String): OAuthAccount?
}
