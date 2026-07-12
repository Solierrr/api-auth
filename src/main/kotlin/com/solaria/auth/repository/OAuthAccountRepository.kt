package com.solaria.auth.repository

import com.solaria.auth.entity.OAuthAccount
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OAuthAccountRepository : JpaRepository<OAuthAccount, UUID> {
    fun findByProviderAndProviderUserId(provider: String, providerUserId: String): OAuthAccount?
}
