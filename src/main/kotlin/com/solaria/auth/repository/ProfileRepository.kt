package com.solaria.auth.repository

import com.solaria.auth.entity.Profile
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProfileRepository : JpaRepository<Profile, UUID> {
    fun existsByCpf(cpf: String): Boolean
}
