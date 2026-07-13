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
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "two_factor")
class TwoFactor(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_user", nullable = false, unique = true)
    var user: UserAccount? = null,

    @Column(nullable = false)
    var secret: String = "",

    @Column(nullable = false)
    var enabled: Boolean = false,

    @Column(name = "backup_codes")
    var backupCodes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,

    @Column(nullable = false)
    var digits: Short = 6,

    @Column(nullable = false)
    var period: Short = 30,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
