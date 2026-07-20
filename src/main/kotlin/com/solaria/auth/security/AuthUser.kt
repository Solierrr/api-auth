package com.solaria.auth.security

import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.AccountStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class AuthUser private constructor(
    val id: UUID,
    private val email: String,
    private val passwordHash: String,
    private val status: AccountStatus,
    private val lockedUntil: java.time.Instant?
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = emptyList()

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonLocked(): Boolean =
        status != AccountStatus.LOCKED && (lockedUntil == null || lockedUntil.isBefore(java.time.Instant.now()))

    override fun isEnabled(): Boolean = status == AccountStatus.ACTIVE

    companion object {
        fun fromLocalCredential(user: UserAccount): AuthUser = from(user, user.localCredential?.passwordHash.orEmpty())

        fun fromBearerToken(user: UserAccount): AuthUser = from(user, "")

        private fun from(user: UserAccount, passwordHash: String): AuthUser = AuthUser(
            id = requireNotNull(user.id),
            email = user.primaryEmail,
            passwordHash = passwordHash,
            status = user.status,
            lockedUntil = user.lockedUntil
        )
    }
}
