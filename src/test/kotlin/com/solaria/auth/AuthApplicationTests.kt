package com.solaria.auth

import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.entity.LocalCredential
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.security.AuthUser
import com.solaria.auth.security.TokenHashing
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthApplicationTests {
    @Test
    fun `token hashing is deterministic`() {
        assertContentEquals(TokenHashing.sha256("token"), TokenHashing.sha256("token"))
        assertFalse(TokenHashing.sha256("token").contentEquals(TokenHashing.sha256("other-token")))
    }

    @Test
    fun `local credential creates an enabled spring security user`() {
        val user = UserAccount(id = UUID.randomUUID(), primaryEmail = "user@example.com")
        user.localCredential = LocalCredential(user = user, passwordHash = "hash")

        val principal = AuthUser.fromLocalCredential(user)

        assertTrue(principal.isEnabled)
        assertTrue(principal.isAccountNonLocked)
        assertTrue(principal.password == "hash")
    }

    @Test
    fun `disabled or temporarily locked accounts cannot authenticate`() {
        val disabled = UserAccount(
            id = UUID.randomUUID(),
            primaryEmail = "disabled@example.com",
            status = AccountStatus.DISABLED
        )
        val locked = UserAccount(
            id = UUID.randomUUID(),
            primaryEmail = "locked@example.com",
            lockedUntil = Instant.now().plusSeconds(60)
        )

        assertFalse(AuthUser.fromBearerToken(disabled).isEnabled)
        assertFalse(AuthUser.fromBearerToken(locked).isAccountNonLocked)
    }
}
