package com.solaria.auth.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "core_provisioning_dlq")
class CoreProvisioningDlq(
    @Id
    @Column(name = "auth_user_id", nullable = false)
    var authUserId: UUID? = null,

    @Column(name = "last_error", nullable = false)
    var lastError: String = "",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
