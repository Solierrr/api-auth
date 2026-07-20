package com.solaria.auth.entity

import com.solaria.auth.enums.TotpAlgorithm
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "totp_factor")
class TotpFactor(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: UserAccount? = null,

    @Column(name = "secret_ciphertext", nullable = false)
    var secretCiphertext: ByteArray = byteArrayOf(),

    @Column(name = "secret_nonce", nullable = false)
    var secretNonce: ByteArray = byteArrayOf(),

    @Column(name = "encryption_key_id", nullable = false, length = 100)
    var encryptionKeyId: String = "",

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "otp_algorithm")
    var algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,

    @Column(nullable = false)
    var digits: Short = 6,

    @Column(name = "period_seconds", nullable = false)
    var periodSeconds: Short = 30,

    @Column(name = "enabled_at")
    var enabledAt: Instant? = null,

    @Column(name = "last_used_counter")
    var lastUsedCounter: Long? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
