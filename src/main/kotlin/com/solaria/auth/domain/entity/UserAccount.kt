package com.solaria.auth.entity

import com.solaria.auth.enums.AccountStatus
import jakarta.persistence.Column
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "auth_user")
class UserAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "primary_email", nullable = false, unique = true, columnDefinition = "citext")
    var primaryEmail: String = "",

    @Column(name = "email_verified_at")
    var emailVerifiedAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "account_status")
    var status: AccountStatus = AccountStatus.ACTIVE,

    @Column(name = "failed_login_attempts", nullable = false)
    var failedLoginAttempts: Int = 0,

    @Column(name = "locked_until")
    var lockedUntil: Instant? = null,

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @OneToOne(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var localCredential: LocalCredential? = null

    @OneToMany(mappedBy = "user")
    var oneTimeTokens: MutableSet<OneTimeToken> = mutableSetOf()

    @OneToMany(mappedBy = "user")
    var sessions: MutableSet<Session> = mutableSetOf()

    @OneToMany(mappedBy = "user")
    var federatedIdentities: MutableSet<FederatedIdentity> = mutableSetOf()

    @OneToOne(mappedBy = "user")
    var totpFactor: TotpFactor? = null
}
