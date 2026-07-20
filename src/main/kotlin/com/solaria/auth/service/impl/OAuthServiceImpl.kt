package com.solaria.auth.service.impl

import com.solaria.auth.entity.OAuthAccount
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.OAuthAccountRepository
import com.solaria.auth.service.OAuthService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class OAuthServiceImpl(private val oauthAccountRepository: OAuthAccountRepository) : OAuthService {
    override fun link(user: UserAccount, provider: String, providerUserId: String): OAuthAccount {
        val existing = oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
        check(existing == null || existing.user?.id == user.id) { "OAuth account is already linked to another user" }
        return existing ?: oauthAccountRepository.save(OAuthAccount(user = user, provider = provider, providerUserId = providerUserId))
    }

    @Transactional(readOnly = true)
    override fun findLinkedAccount(provider: String, providerUserId: String): OAuthAccount? =
        oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
}
