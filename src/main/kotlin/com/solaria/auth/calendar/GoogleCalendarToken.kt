package com.solaria.auth.calendar

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

enum class GoogleCalendarTokenStatus { CONNECTED, DISCONNECTED, REVOKED }

@Entity
@Table(name = "google_calendar_token")
class GoogleCalendarToken(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "technician_id", nullable = false, unique = true, length = 100)
    var technicianId: String = "",

    @Column(name = "access_token")
    var accessToken: ByteArray? = null,

    @Column(name = "refresh_token")
    var refreshToken: ByteArray? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: GoogleCalendarTokenStatus = GoogleCalendarTokenStatus.CONNECTED,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    @Column(nullable = false)
    var version: Long = 0
)
