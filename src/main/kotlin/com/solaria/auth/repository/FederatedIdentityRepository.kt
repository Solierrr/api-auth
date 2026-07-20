package com.solaria.auth.repository

import com.solaria.auth.entity.FederatedIdentity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FederatedIdentityRepository : JpaRepository<FederatedIdentity, UUID> {
    fun findByIssuerAndSubject(issuer: String, subject: String): FederatedIdentity?
    fun findByUserIdAndIssuer(userId: UUID, issuer: String): FederatedIdentity?
}
