package com.solaria.auth.service

import com.solaria.auth.entity.TotpFactor
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.TotpAlgorithm

interface TwoFactorService {
    fun configure(
        user: UserAccount,
        secretCiphertext: ByteArray,
        secretNonce: ByteArray,
        encryptionKeyId: String,
        algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
        digits: Short = 6,
        periodSeconds: Short = 30
    ): TotpFactor

    fun enable(user: UserAccount): TotpFactor
    fun disable(user: UserAccount): TotpFactor
    fun findByUser(user: UserAccount): TotpFactor?
}
