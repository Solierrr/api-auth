package com.solaria.auth.security

import com.solaria.auth.repository.UserAccountRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class AccountUserDetailsService(
    private val userAccountRepository: UserAccountRepository
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails =
        userAccountRepository.findByEmailIgnoreCase(username)?.let(AuthUser::from)
            ?: throw UsernameNotFoundException("User account not found")
}
