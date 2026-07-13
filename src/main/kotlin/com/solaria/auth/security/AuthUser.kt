package com.solaria.auth.security

import com.solaria.auth.entity.UserAccount
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class AuthUser private constructor(
    val id: UUID,
    private val email: String,
    private val passwordHash: String,
    private val active: Boolean,
    private val locked: Boolean
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonLocked(): Boolean = !locked

    override fun isEnabled(): Boolean = active

    companion object {
        fun from(user: UserAccount): AuthUser = AuthUser(
            id = requireNotNull(user.id),
            email = user.email,
            passwordHash = user.passwordHash,
            active = user.isActive,
            locked = user.isLocked
        )
    }
}
