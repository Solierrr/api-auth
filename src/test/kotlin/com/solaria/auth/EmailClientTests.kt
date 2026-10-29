package com.solaria.auth

import com.solaria.auth.integration.email.EmailClient
import com.solaria.auth.integration.email.EmailClientProperties
import com.solaria.auth.integration.email.EmailDeliveryException
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailClientTests {
    private class Captured(val method: String, val path: String, val apiKey: String?, val contentType: String?, val body: String)

    private val captured = AtomicReference<Captured>()
    private var status = 200
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            captured.set(Captured(exchange.requestMethod, exchange.requestURI.path, exchange.requestHeaders.getFirst("X-API-Key"),
                exchange.requestHeaders.getFirst("Content-Type"), exchange.requestBody.readBytes().toString(Charsets.UTF_8)))
            val response = """{"status":"accepted","messageId":"id"}""".toByteArray()
            exchange.sendResponseHeaders(status, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        start()
    }
    private val url = "http://127.0.0.1:${server.address.port}"
    private fun client(baseUrl: String = url, apiKey: String = "k".repeat(32)) =
        EmailClient(EmailClientProperties(baseUrl, apiKey, Duration.ofSeconds(2), Duration.ofSeconds(2)))

    @AfterEach
    fun stop() = server.stop(0)

    @Test
    fun `posts the template to the email service with the internal key`() {
        client("$url/").send("password-reset", "user@example.com", "https://app.example.com/pt-BR/redefinir-senha?token=abc")

        val request = requireNotNull(captured.get())
        assertEquals("POST", request.method)
        assertEquals("/emails", request.path)
        assertEquals("k".repeat(32), request.apiKey)
        assertTrue(requireNotNull(request.contentType).startsWith("application/json"))
        assertEquals("""{"template":"password-reset","locale":"pt-BR","to":"user@example.com","data":{"actionUrl":"https://app.example.com/pt-BR/redefinir-senha?token=abc"}}""", request.body)
    }

    @Test
    fun `a rejected request fails without leaking the recipient`() {
        status = 503
        val failure = assertFailsWith<EmailDeliveryException> { client().send("email-verification", "user@example.com", "https://app.example.com/x") }
        assertTrue(requireNotNull(failure.message).contains("503"))
        assertFalse(failure.message!!.contains("user@example.com"))
    }

    @Test
    fun `an unreachable service is a delivery failure`() {
        server.stop(0)
        assertFailsWith<EmailDeliveryException> { client().send("password-reset", "user@example.com", "https://app.example.com/x") }
    }

    @Test
    fun `without url or key it is not configured and never sends`() {
        listOf(client(baseUrl = ""), client(apiKey = "")).forEach {
            assertFalse(it.configured)
            assertFailsWith<EmailDeliveryException> { it.send("password-reset", "user@example.com", "https://app.example.com/x") }
        }
        assertEquals(null, captured.get())
        assertTrue(client().configured)
    }

    private fun runner() = ApplicationContextRunner()
        .withInitializer(ConfigDataApplicationContextInitializer())
        .withUserConfiguration(EmailClient::class.java)

    @Test
    fun `the environment variables bind to the client, with the shared names as fallback`() {
        runner().withPropertyValues("EMAIL_SERVICE_URL=http://email.test", "EMAIL_SERVICE_API_KEY=" + "a".repeat(32)).run {
            assertTrue(it.getBean(EmailClient::class.java).configured)
        }
        runner().withPropertyValues("API_EMAIL_URL=http://email.test", "SERVICE_KEY_API_EMAIL=" + "a".repeat(32)).run {
            assertTrue(it.getBean(EmailClient::class.java).configured)
        }
        runner().withPropertyValues("API_EMAIL_URL=http://email.test").run {
            assertFalse(it.getBean(EmailClient::class.java).configured)
        }
        runner().run { assertFalse(it.getBean(EmailClient::class.java).configured) }
    }
}
