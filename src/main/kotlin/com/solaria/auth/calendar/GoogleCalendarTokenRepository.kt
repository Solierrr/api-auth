package com.solaria.auth.calendar

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GoogleCalendarTokenRepository : JpaRepository<GoogleCalendarToken, UUID> {
    fun findByTechnicianId(technicianId: String): GoogleCalendarToken?
}
