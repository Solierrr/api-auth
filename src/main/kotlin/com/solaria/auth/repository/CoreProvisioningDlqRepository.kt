package com.solaria.auth.repository

import com.solaria.auth.domain.entity.CoreProvisioningDlq
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CoreProvisioningDlqRepository : JpaRepository<CoreProvisioningDlq, UUID>
