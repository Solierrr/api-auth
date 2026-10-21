package com.solaria.auth.mobile

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface MobileTransactionRepository : JpaRepository<MobileTransaction, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from MobileTransaction t where t.id = :id")
    fun lockById(id: String): MobileTransaction?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findByCodeHash(codeHash: ByteArray): MobileTransaction?
}
