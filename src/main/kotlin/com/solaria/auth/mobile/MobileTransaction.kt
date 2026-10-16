package com.solaria.auth.mobile

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mobile_auth_transaction")
class MobileTransaction(
    @Id var id: String = "",
    @Column(name = "client_id", nullable = false) var clientId: String = "",
    @Column(name = "redirect_uri", nullable = false) var redirectUri: String = "",
    @Column(nullable = false) var state: String = "",
    @Column(name = "code_challenge", nullable = false) var codeChallenge: String = "",
    @Column(nullable = false) var status: String = "PENDING_AUTHENTICATION",
    @Column(name = "expires_at", nullable = false) var expiresAt: Instant = Instant.now(),
    @Column(name = "user_id") var userId: UUID? = null,
    @Column(name = "authorizing_session_id") var authorizingSessionId: UUID? = null,
    @Column(name = "code_hash", unique = true) var codeHash: ByteArray? = null,
    @Column(name = "code_expires_at") var codeExpiresAt: Instant? = null,
    @Column(name = "consumed_at") var consumedAt: Instant? = null
)
