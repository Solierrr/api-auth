package com.solaria.auth.entity

import com.solaria.auth.enums.OneTimeTokenType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "one_time_token")
class OneTimeToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_user_id", nullable = false)
    var user: UserAccount? = null,

    @Column(name = "token_hash", nullable = false, unique = true)
    var tokenHash: ByteArray = byteArrayOf(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    var type: OneTimeTokenType = OneTimeTokenType.EMAIL_VERIFICATION,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
