package com.solaria.auth.security

import com.solaria.auth.repository.UserAccountRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountUserDetailsService(
    private val userAccountRepository: UserAccountRepository
) : UserDetailsService {
    @Transactional(readOnly = true)
    override fun loadUserByUsername(username: String): UserDetails =
        userAccountRepository.findByPrimaryEmail(username.trim().lowercase())?.let(AuthUser::fromLocalCredential)
            ?: throw UsernameNotFoundException("User account not found")
}
