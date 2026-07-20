package com.solaria.auth.service

import com.solaria.auth.entity.TwoFactor
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.TotpAlgorithm

interface TwoFactorService {
    fun configure(user: UserAccount, secret: String, algorithm: TotpAlgorithm = TotpAlgorithm.SHA1, digits: Short = 6, period: Short = 30): TwoFactor
    fun enable(user: UserAccount): TwoFactor
    fun disable(user: UserAccount): TwoFactor
    fun findByUser(user: UserAccount): TwoFactor?
}
