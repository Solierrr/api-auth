package com.solaria.auth.repository

import com.solaria.auth.entity.SecurityEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SecurityEventRepository : JpaRepository<SecurityEvent, UUID>
