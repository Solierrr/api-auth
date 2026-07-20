package com.solaria.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_account")
class UserAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, unique = true, columnDefinition = "citext")
    var email: String = "",

    @Column(name = "password_hash", nullable = false)
    var passwordHash: String = "",

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "is_locked", nullable = false)
    var isLocked: Boolean = false,

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @OneToOne(mappedBy = "user")
    var profile: Profile? = null

    @OneToMany(mappedBy = "user")
    var tokens: MutableSet<Token> = mutableSetOf()

    @OneToMany(mappedBy = "user")
    var refreshTokens: MutableSet<RefreshToken> = mutableSetOf()

    @OneToMany(mappedBy = "user")
    var sessions: MutableSet<Session> = mutableSetOf()

    @OneToMany(mappedBy = "user")
    var oauthAccounts: MutableSet<OAuthAccount> = mutableSetOf()

    @OneToOne(mappedBy = "user")
    var twoFactor: TwoFactor? = null
}
