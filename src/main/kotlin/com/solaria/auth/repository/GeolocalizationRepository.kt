package com.solaria.auth.repository

import com.solaria.auth.entity.Geolocalization
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface GeolocalizationRepository : JpaRepository<Geolocalization, UUID>
