package com.solaria.auth

import com.solaria.auth.entity.UserAccount
import com.solaria.auth.entity.TotpFactor
import com.solaria.auth.enums.AccountStatus
import com.solaria.auth.mobile.MobileAuthorizationService
import com.solaria.auth.mobile.MobileEligibilityService
import com.solaria.auth.mobile.MobileFlowException
import com.solaria.auth.mobile.QaMobileSessionService
import com.solaria.auth.repository.UserAccountRepository
import com.solaria.auth.repository.TotpFactorRepository
import com.solaria.auth.service.AuthenticationAttemptService
import com.solaria.auth.service.UserService
import com.solaria.auth.service.AuthSession
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.env.MockEnvironment
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QaMobileSessionServiceTests {
    private val users = Mockito.mock(UserAccountRepository::class.java)
    private val factors = Mockito.mock(TotpFactorRepository::class.java)
    private val eligibility = Mockito.mock(MobileEligibilityService::class.java)
    private val mobile = Mockito.mock(MobileAuthorizationService::class.java)
    private val attempts = Mockito.mock(AuthenticationAttemptService::class.java)
    private val userService = Mockito.mock(UserService::class.java)
    private val token = "a".repeat(43)
    private val user = UserAccount(id = UUID.randomUUID(), primaryEmail = "seed@example.test", emailVerifiedAt = Instant.now())
    private fun service(enabled: Boolean = true, deployment: String = "qa", tokens: String = token, email: String = user.primaryEmail, profile: String? = null): QaMobileSessionService {
        val env = MockEnvironment()
        profile?.let { env.setActiveProfiles(it) }
        return QaMobileSessionService(users, factors, eligibility, mobile, attempts, userService, env, enabled, deployment, email, tokens)
    }
    private fun account() { Mockito.`when`(users.findByPrimaryEmail(user.primaryEmail)).thenReturn(user) }

    @Test fun `disabled or non QA deployments never look up accounts`() {
        listOf(service(enabled = false), service(deployment = "prod"), service(deployment = "local"), service(profile = "prod"), service(profile = "production")).forEach {
            assertEquals("QA_LOGIN_DISABLED", assertFailsWith<MobileFlowException> { it.login(token, null, null) }.code)
        }
        Mockito.verifyNoInteractions(users, mobile)
    }
    @Test fun `missing invalid or partially invalid configuration fails closed`() {
        listOf(service(tokens = ""), service(tokens = "short"), service(tokens = "$token,short"), service(email = "")).forEach {
            assertEquals("QA_LOGIN_NOT_CONFIGURED", assertFailsWith<MobileFlowException> { it.login(token, null, null) }.code)
        }
        Mockito.verifyNoInteractions(users, mobile)
    }
    @Test fun `missing malformed and unknown device credentials do not issue sessions`() {
        listOf(null, "short", "b".repeat(43)).forEach {
            assertEquals("QA_DEVICE_NOT_AUTHORIZED", assertFailsWith<MobileFlowException> { service().login(it, null, null) }.code)
        }
        Mockito.verifyNoInteractions(users, mobile)
    }
    @Test fun `inactive locked and unverified accounts cannot log in`() {
        account()
        user.emailVerifiedAt = null
        assertFailsWith<MobileFlowException> { service().login(token, null, null) }
        user.emailVerifiedAt = Instant.now()
        user.lockedUntil = Instant.now().plusSeconds(60)
        assertFailsWith<MobileFlowException> { service().login(token, null, null) }
        user.lockedUntil = null
        user.status = AccountStatus.DISABLED
        assertFailsWith<MobileFlowException> { service().login(token, null, null) }
        Mockito.verifyNoInteractions(mobile)
    }
    @Test fun `eligible account uses shared mobile session issuance and any authorized device`() {
        account()
        val methods = arrayOf("qa_device")
        val expected = AuthSession("access", "refresh", Instant.now(), requireNotNull(user.id), user.primaryEmail)
        Mockito.`when`(mobile.issueSession(user, methods, "ip", "agent")).thenReturn(expected)
        assertEquals(expected, service(tokens = "${"b".repeat(43)},$token").login(token, "ip", "agent"))
        Mockito.verify(eligibility).requireEligible(requireNotNull(user.id))
        Mockito.verify(attempts).recordSuccess(user, "ip", "agent", "qa_device")
        Mockito.verify(mobile).issueSession(user, methods, "ip", "agent")
    }
    @Test fun `ineligible and MFA enabled accounts never receive sessions`() {
        account()
        Mockito.doThrow(MobileFlowException("MOBILE_ACCOUNT_NOT_ALLOWED")).`when`(eligibility).requireEligible(requireNotNull(user.id))
        assertFailsWith<MobileFlowException> { service().login(token, null, null) }
        Mockito.`when`(factors.findByUserId(requireNotNull(user.id))).thenReturn(TotpFactor(user = user, enabledAt = Instant.now()))
        assertEquals("QA_LOGIN_MFA_UNSUPPORTED", assertFailsWith<MobileFlowException> { service().login(token, null, null) }.code)
        Mockito.verifyNoInteractions(mobile)
    }
}

