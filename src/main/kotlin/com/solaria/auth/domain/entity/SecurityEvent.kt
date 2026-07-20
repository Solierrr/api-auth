package com.solaria.auth.entity

import com.solaria.auth.enums.SecurityEventType
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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "security_event")
class SecurityEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: UserAccount? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    var session: Session? = null,

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "event_type", nullable = false, columnDefinition = "security_event_type")
    var eventType: SecurityEventType = SecurityEventType.LOGIN_SUCCEEDED,

    @Column(nullable = false)
    var succeeded: Boolean = true,

    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null,

    @Column(name = "user_agent")
    var userAgent: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var details: String? = null,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = Instant.now()
)
