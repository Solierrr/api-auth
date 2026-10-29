package com.solaria.auth

import com.solaria.auth.entity.LocalCredential
import com.solaria.auth.entity.OneTimeToken
import com.solaria.auth.entity.UserAccount
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.enums.OneTimeTokenType
import com.solaria.auth.integration.email.EmailClient
import com.solaria.auth.integration.email.EmailDeliveryException
import com.solaria.auth.mobile.MobileFlowException
import com.solaria.auth.repository.OneTimeTokenRepository
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.security.TokenHashing
import com.solaria.auth.service.EmailCredentialService
import com.solaria.auth.service.SessionService
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmailCredentialServiceTests {
    private class Sent(val template: String, val to: String, val link: String)

    private val users = Mockito.mock(UserAccountRepository::class.java)
    private val tokens = Mockito.mock(OneTimeTokenRepository::class.java)
    private val passwords = Mockito.mock(PasswordEncoder::class.java)
    private val sessions = Mockito.mock(SessionService::class.java)
    private val email = Mockito.mock(EmailClient::class.java)
    private val resetUrl = "https://app.example.com/pt-BR/redefinir-senha"
    private val verificationUrl = "https://app.example.com/pt-BR/verificar-email"
    private val saved = mutableListOf<OneTimeToken>()
    private val sent = mutableListOf<Sent>()
    private val user = UserAccount(id = UUID.randomUUID(), primaryEmail = "user@example.com").also {
        it.localCredential = LocalCredential(user = it, passwordHash = "old-hash")
    }
    private val strongPassword = "a-long-new-password"

    init {
        Mockito.`when`(email.configured).thenReturn(true)
        Mockito.doAnswer { sent += Sent(it.getArgument(0), it.getArgument(1), it.getArgument(2)); null }
            .`when`(email).send(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
        Mockito.`when`(tokens.save(Mockito.any(OneTimeToken::class.java))).thenAnswer { (it.arguments[0] as OneTimeToken).also(saved::add) }
        Mockito.`when`(users.findByPrimaryEmailForUpdate(user.primaryEmail)).thenReturn(user)
        Mockito.`when`(users.findById(requireNotNull(user.id))).thenReturn(Optional.of(user))
        Mockito.`when`(passwords.encode(strongPassword)).thenReturn("new-hash")
    }

    private fun service() = EmailCredentialService(users, tokens, passwords, sessions, email, verificationUrl, resetUrl)
    private fun tokenOf(link: String) = link.substringAfter("?token=")
    private fun stored(token: String, type: OneTimeTokenType, expiresIn: Long = 600) =
        OneTimeToken(user = user, type = type, tokenHash = TokenHashing.sha256(token), expiresAt = Instant.now().plusSeconds(expiresIn)).also {
            Mockito.`when`(tokens.findByTokenHashAndType(TokenHashing.sha256(token), type)).thenReturn(it)
            Mockito.`when`(tokens.save(it)).thenReturn(it)
        }

    @Test
    fun `reset request mails a single use link and stores only the token hash for thirty minutes`() {
        service().requestReset(" User@Example.com ")

        val mail = sent.single()
        assertEquals("password-reset", mail.template)
        assertEquals("user@example.com", mail.to)
        assertTrue(mail.link.startsWith("$resetUrl?token="))
        val token = tokenOf(mail.link)
        assertEquals(43, token.length)
        val record = saved.single()
        assertEquals(OneTimeTokenType.PASSWORD_RESET, record.type)
        assertContentEquals(TokenHashing.sha256(token), record.tokenHash)
        assertNull(record.consumedAt)
        assertTrue(record.expiresAt.isAfter(Instant.now().plusSeconds(1700)) && record.expiresAt.isBefore(Instant.now().plusSeconds(1900)))
    }

    @Test
    fun `reset request answers the same for unknown, disabled and passwordless accounts`() {
        val disabled = UserAccount(id = UUID.randomUUID(), primaryEmail = "off@example.com", status = AccountStatus.DISABLED)
            .also { it.localCredential = LocalCredential(user = it, passwordHash = "hash") }
        val federated = UserAccount(id = UUID.randomUUID(), primaryEmail = "google@example.com")
        Mockito.`when`(users.findByPrimaryEmailForUpdate("off@example.com")).thenReturn(disabled)
        Mockito.`when`(users.findByPrimaryEmailForUpdate("google@example.com")).thenReturn(federated)

        listOf("nobody@example.com", "off@example.com", "google@example.com").forEach { service().requestReset(it) }

        assertTrue(sent.isEmpty())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `a second reset request within a minute does not mail again`() {
        Mockito.`when`(tokens.findFirstByUserIdAndTypeOrderByCreatedAtDesc(requireNotNull(user.id), OneTimeTokenType.PASSWORD_RESET))
            .thenReturn(OneTimeToken(user = user, type = OneTimeTokenType.PASSWORD_RESET, createdAt = Instant.now().minusSeconds(10)))

        service().requestReset(user.primaryEmail)

        assertTrue(sent.isEmpty())
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `without the email service nothing is looked up or sent`() {
        Mockito.`when`(email.configured).thenReturn(false)

        val failure = assertFailsWith<MobileFlowException> { service().requestReset(user.primaryEmail) }

        assertEquals("EMAIL_NOT_CONFIGURED", failure.code)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.status)
        Mockito.verifyNoInteractions(tokens)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `a missing link url also disables the request`() {
        val failure = assertFailsWith<MobileFlowException> {
            EmailCredentialService(users, tokens, passwords, sessions, email, verificationUrl, "").requestReset(user.primaryEmail)
        }
        assertEquals("EMAIL_NOT_CONFIGURED", failure.code)
    }

    @Test
    fun `a delivery failure is reported as unavailable`() {
        Mockito.doThrow(EmailDeliveryException("down")).`when`(email).send(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())

        val failure = assertFailsWith<MobileFlowException> { service().requestReset(user.primaryEmail) }

        assertEquals("EMAIL_UNAVAILABLE", failure.code)
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.status)
    }

    @Test
    fun `confirming a reset changes the password, burns the token and revokes every session`() {
        val token = "t".repeat(43)
        val record = stored(token, OneTimeTokenType.PASSWORD_RESET)

        service().reset(token, strongPassword)

        assertEquals("new-hash", user.localCredential?.passwordHash)
        assertNotNull(record.consumedAt)
        Mockito.verify(sessions).revokeAllFor(user)
        val replay = assertFailsWith<MobileFlowException> { service().reset(token, strongPassword) }
        assertEquals("INVALID_OR_EXPIRED_TOKEN", replay.code)
        Mockito.verify(sessions, Mockito.times(1)).revokeAllFor(user)
    }

    @Test
    fun `expired, unknown and malformed tokens are rejected without touching the password`() {
        stored("e".repeat(43), OneTimeTokenType.PASSWORD_RESET, expiresIn = -1)

        listOf("e".repeat(43), "u".repeat(43), "short").forEach {
            val failure = assertFailsWith<MobileFlowException> { service().reset(it, strongPassword) }
            assertEquals("INVALID_OR_EXPIRED_TOKEN", failure.code)
        }

        assertEquals("old-hash", user.localCredential?.passwordHash)
        Mockito.verifyNoInteractions(sessions)
    }

    @Test
    fun `a verification token cannot reset a password`() {
        val token = "v".repeat(43)
        stored(token, OneTimeTokenType.EMAIL_VERIFICATION)

        assertFailsWith<MobileFlowException> { service().reset(token, strongPassword) }

        assertEquals("old-hash", user.localCredential?.passwordHash)
    }

    @Test
    fun `a weak password is rejected before the token is consumed`() {
        val token = "w".repeat(43)
        val record = stored(token, OneTimeTokenType.PASSWORD_RESET)

        val failure = assertFailsWith<MobileFlowException> { service().reset(token, "short") }

        assertEquals("PASSWORD_POLICY", failure.code)
        assertNull(record.consumedAt)
    }

    @Test
    fun `verification request mails the verification template while the account is unverified`() {
        service().requestVerification(requireNotNull(user.id))

        val mail = sent.single()
        assertEquals("email-verification", mail.template)
        assertTrue(mail.link.startsWith("$verificationUrl?token="))
        assertEquals(OneTimeTokenType.EMAIL_VERIFICATION, saved.single().type)
    }

    @Test
    fun `a verified account is not mailed again`() {
        user.emailVerifiedAt = Instant.now()

        service().requestVerification(requireNotNull(user.id))

        assertTrue(sent.isEmpty())
    }

    @Test
    fun `confirming a verification token marks the email as verified`() {
        val token = "f".repeat(43)
        stored(token, OneTimeTokenType.EMAIL_VERIFICATION)

        service().confirmVerification(token)

        assertNotNull(user.emailVerifiedAt)
    }
}
