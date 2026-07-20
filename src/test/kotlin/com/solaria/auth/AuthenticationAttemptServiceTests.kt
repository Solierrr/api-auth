package com.solaria.auth

import com.solaria.auth.entity.SecurityEvent
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.repository.SecurityEventRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.service.AuthenticationAttemptService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthenticationAttemptServiceTests {
    private lateinit var userRepository: UserAccountRepository
    private lateinit var securityEventRepository: SecurityEventRepository
    private lateinit var service: AuthenticationAttemptService

    @BeforeEach
    fun setUp() {
        userRepository = Mockito.mock(UserAccountRepository::class.java)
        securityEventRepository = Mockito.mock(SecurityEventRepository::class.java)
        service = AuthenticationAttemptService(userRepository, securityEventRepository)
    }

    @Test
    fun `fifth failed attempt temporarily locks account and records event`() {
        val user = UserAccount(
            id = UUID.randomUUID(),
            primaryEmail = "user@example.com",
            failedLoginAttempts = 4
        )
        Mockito.`when`(userRepository.findByPrimaryEmailForUpdate("user@example.com")).thenReturn(user)

        service.recordFailure(" USER@example.com ", "127.0.0.1", "test-agent", "password")

        assertEquals(5, user.failedLoginAttempts)
        assertNotNull(user.lockedUntil)
        val event = ArgumentCaptor.forClass(SecurityEvent::class.java)
        Mockito.verify(securityEventRepository).save(event.capture())
        assertEquals(false, event.value.succeeded)
    }

    @Test
    fun `successful authentication clears failed attempts and lock`() {
        val user = UserAccount(
            id = UUID.randomUUID(),
            primaryEmail = "user@example.com",
            failedLoginAttempts = 4,
            lockedUntil = Instant.now().minusSeconds(1)
        )

        service.recordSuccess(user, null, null, "password")

        assertEquals(0, user.failedLoginAttempts)
        assertNull(user.lockedUntil)
        Mockito.verify(userRepository).save(user)
        Mockito.verify(securityEventRepository).save(Mockito.any(SecurityEvent::class.java))
    }
}
