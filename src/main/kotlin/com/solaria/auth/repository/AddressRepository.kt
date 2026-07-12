package com.solaria.auth.repository

import com.solaria.auth.entity.Address
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AddressRepository : JpaRepository<Address, UUID>
