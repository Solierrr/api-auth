package com.solaria.auth

import com.solaria.auth.browser.BrowserSessionCookies
import com.solaria.auth.browser.BrowserSessionController
import com.solaria.auth.dto.auth.request.LoginRequest
import com.solaria.auth.repository.RefreshTokenRepository
import com.solaria.auth.security.config.JwtProperties
import com.solaria.auth.service.AuthService
import com.solaria.auth.service.AuthSession
import com.solaria.auth.service.FirebaseAuthenticationService
import com.solaria.auth.service.SessionService
import com.solaria.auth.service.InvalidRefreshTokenException
import com.solaria.auth.security.config.CorsConfig
import com.solaria.auth.entity.RefreshToken
import com.solaria.auth.entity.Session
import com.solaria.auth.security.TokenHashing
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.server.ResponseStatusException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BrowserSessionTests {
    private val properties = JwtProperties("test", Duration.ofMinutes(15), Duration.ofDays(7), "", "", "")
    private val cookies = BrowserSessionCookies(properties, "https://qa.solarianetwork.site", true)
    private val auth = Mockito.mock(AuthService::class.java)
    private val firebase = Mockito.mock(FirebaseAuthenticationService::class.java)
    private val tokens = Mockito.mock(RefreshTokenRepository::class.java)
    private val sessions = Mockito.mock(SessionService::class.java)
    private val controller = BrowserSessionController(auth, firebase, cookies, tokens, sessions)
    private fun request() = MockHttpServletRequest().apply {
        addHeader("Origin", "https://qa.solarianetwork.site")
        addHeader("X-Solaria-Session", "1")
    }
    private fun session() = AuthSession("access", "private-refresh", Instant.now().plusSeconds(900), UUID.randomUUID(), "qa@test.dev")
    private fun withCookie(value: String): MockHttpServletRequest {
        val request = request(); val response = MockHttpServletResponse()
        cookies.write(request, response, value)
        request.setCookies(Cookie(response.getHeader("Set-Cookie")!!.substringBefore('='), value))
        return request
    }

    @Test fun `cookie is private secure partitioned host only and expires`() {
        val response = MockHttpServletResponse()
        cookies.write(request(), response, "private-refresh")
        val header = response.getHeader("Set-Cookie")!!
        assertTrue(header.contains("HttpOnly")); assertTrue(header.contains("Secure"))
        assertTrue(header.contains("Partitioned")); assertTrue(header.contains("SameSite=None"))
        assertTrue(header.contains("Path=/auth/browser")); assertFalse(header.contains("Domain="))
        assertTrue(header.contains("Max-Age=604800"))
    }
    @Test fun `cookie is removed with matching scope`() {
        val response = MockHttpServletResponse(); cookies.clear(request(), response)
        assertTrue(response.getHeader("Set-Cookie")!!.contains("Max-Age=0"))
    }
    @Test fun `untrusted or absent browser origin is rejected`() {
        assertFailsWith<ResponseStatusException> { cookies.requireBrowser(MockHttpServletRequest(), MockHttpServletResponse()) }
        val request = request().apply { removeHeader("Origin"); addHeader("Origin", "https://evil.test") }
        assertFailsWith<ResponseStatusException> { cookies.requireBrowser(request, MockHttpServletResponse()) }
    }
    @Test fun `custom header is required even for allowed origins`() {
        val request = request().apply { removeHeader("X-Solaria-Session") }
        assertFailsWith<ResponseStatusException> { cookies.requireBrowser(request, MockHttpServletResponse()) }
    }
    @Test fun `login returns access information but never the refresh token in JSON`() {
        Mockito.`when`(auth.login("qa@test.dev", "password", "127.0.0.1", null)).thenReturn(session())
        val response = MockHttpServletResponse()
        val result = controller.login(LoginRequest("qa@test.dev", "password"), request(), response)
        assertEquals("access", result.accessToken)
        assertFalse(result.javaClass.declaredFields.any { it.name == "refreshToken" })
        assertEquals("no-store", response.getHeader("Cache-Control"))
    }
    @Test fun `refresh without a cookie is anonymous without database calls`() {
        assertEquals(204, controller.refresh(request(), MockHttpServletResponse()).statusCode.value())
        Mockito.verifyNoInteractions(auth)
    }
    @Test fun `refresh reads and rotates only the http cookie`() {
        val request = withCookie("old-refresh")
        Mockito.`when`(auth.refresh("old-refresh", "127.0.0.1", null)).thenReturn(session())
        val response = MockHttpServletResponse()
        val result = controller.refresh(request, response)
        assertEquals(200, result.statusCode.value()); assertFalse(result.body!!.javaClass.declaredFields.any { it.name == "refreshToken" })
        assertTrue(response.getHeader("Set-Cookie")!!.contains("private-refresh"))
    }
    @Test fun `invalid refresh cookie is expired`() {
        val request = withCookie("invalid-refresh")
        Mockito.`when`(auth.refresh("invalid-refresh", "127.0.0.1", null)).thenThrow(InvalidRefreshTokenException())
        val response = MockHttpServletResponse()
        assertFailsWith<InvalidRefreshTokenException> { controller.refresh(request, response) }
        assertTrue(response.getHeader("Set-Cookie")!!.contains("Max-Age=0"))
    }
    @Test fun `only browser endpoints accept credentialed cors`() {
        val source = CorsConfig("https://qa.solarianetwork.site").corsConfigurationSource()
        val browser = source.getCorsConfiguration(MockHttpServletRequest("POST", "/auth/browser/refresh"))!!
        assertEquals(true, browser.allowCredentials)
        assertTrue(browser.allowedHeaders!!.contains("X-Solaria-Session"))
        val legacy = source.getCorsConfiguration(MockHttpServletRequest("POST", "/auth/login"))!!
        assertEquals(false, legacy.allowCredentials)
    }
    @Test fun `logout revokes only the cookie session and clears the cookie`() {
        val request = withCookie("current-refresh")
        val session = Session()
        Mockito.`when`(tokens.findByTokenHash(TokenHashing.sha256("current-refresh"))).thenReturn(RefreshToken(session = session))
        val response = MockHttpServletResponse()
        assertEquals(204, controller.logout(request, response).statusCode.value())
        Mockito.verify(sessions).revoke(session, "BROWSER_LOGOUT")
        Mockito.verifyNoInteractions(auth)
        assertTrue(response.getHeader("Set-Cookie")!!.contains("Max-Age=0"))
    }
    @Test fun `different frontend origins do not share refresh cookies`() {
        val isolated = BrowserSessionCookies(properties, "https://qa.solarianetwork.site,https://app.solarianetwork.site", true)
        val response = MockHttpServletResponse(); isolated.write(request(), response, "sandbox-refresh")
        val other = MockHttpServletRequest().apply {
            addHeader("Origin", "https://app.solarianetwork.site")
            setCookies(Cookie(response.getHeader("Set-Cookie")!!.substringBefore('='), "sandbox-refresh"))
        }
        assertEquals(null, isolated.read(other))
    }
}
