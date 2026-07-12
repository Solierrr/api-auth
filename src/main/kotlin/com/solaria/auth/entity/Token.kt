package com.solaria.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
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
@Table(name = "token")
class Token(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_user", nullable = false)
    var user: UserAccount? = null,

    @Column(nullable = false, unique = true, length = 255)
    var token: String = "",

    @Column(nullable = false, length = 30)
    var type: String = "",

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = Instant.now(),

    @Column(nullable = false)
    var used: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
