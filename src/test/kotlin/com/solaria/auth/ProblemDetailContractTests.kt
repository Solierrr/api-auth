package com.solaria.auth

import com.solaria.auth.controller.AuthExceptionHandler
import com.solaria.auth.exception.handler.ProblemDetailFactory
import com.solaria.auth.mobile.MobileExceptionHandler
import com.solaria.auth.mobile.MobileFlowException
import com.solaria.auth.security.ProblemDetailAccessDeniedHandler
import com.solaria.auth.security.ProblemDetailAuthenticationEntryPoint
import com.solaria.auth.service.AccountLinkRequiredException
import com.solaria.auth.service.EmailAlreadyRegisteredException
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.server.ResponseStatusException
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProblemDetailContractTests {
    private val factory = ProblemDetailFactory()
    private val request = MockHttpServletRequest("POST", "/auth/login")
    private val handler = AuthExceptionHandler(request, factory)

    fun validationEndpoint(payload: String) = payload

    @RestController
    class ErrorEndpoint {
        @GetMapping("/auth/test-error")
        fun fail(): Nothing = throw EmailAlreadyRegisteredException()
    }

    @Test
    fun `HTTP responses serialize status and stable error fields at the top level`() {
        MockMvcBuilders.standaloneSetup(ErrorEndpoint()).setControllerAdvice(handler).build()
            .perform(get("/auth/test-error"))
            .andExpect(status().isConflict)
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_REGISTERED"))
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"))
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `validation errors identify fields without including submitted values`() {
        val result = BeanPropertyBindingResult("private submitted value", "payload")
        result.addError(FieldError("payload", "email", "Invalid email"))
        val method = javaClass.getMethod("validationEndpoint", String::class.java)
        val response = handler.validationError(MethodArgumentNotValidException(MethodParameter(method, 0), result))
        checkContract(requireNotNull(response.body), 400, "VALIDATION_ERROR")
        assertEquals("Erro de validação nos campos da requisição.", response.body?.detail)
        assertEquals(listOf(mapOf("field" to "email", "message" to "Invalid email")), response.body?.properties?.get("errors"))
    }

    @Test
    fun `logs preserve the response error code as a structured field`() {
        val logger = org.slf4j.LoggerFactory.getLogger(AuthExceptionHandler::class.java) as ch.qos.logback.classic.Logger
        val appender = ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>()
        appender.start()
        logger.addAppender(appender)
        try {
            val response = handler.invalidCredentials(BadCredentialsException("private diagnostic"))
            val event = appender.list.first()
            assertEquals(ch.qos.logback.classic.Level.WARN, event.level)
            assertEquals(response.body?.properties?.get("errorCode"), event.keyValuePairs.first { it.key == "errorCode" }.value)
        } finally {
            logger.detachAppender(appender)
            appender.stop()
        }
    }

    private fun checkContract(body: ProblemDetail, status: Int, code: String) {
        assertEquals(status, body.status)
        // title é a reason phrase do status (Spring), não o errorCode
        assertEquals(HttpStatus.valueOf(status).reasonPhrase, body.title)
        assertEquals(code, body.properties?.get("errorCode"))
        assertNotNull(body.properties?.get("timestamp"))
        assertEquals(URI.create("/auth/login"), body.instance)
        assertNull(body.type)
    }

    @Test
    fun `credentials and duplicate email keep specific codes and HTTP status`() {
        val credentials = handler.invalidCredentials(BadCredentialsException("private diagnostic"))
        checkContract(requireNotNull(credentials.body), 401, "INVALID_CREDENTIALS")
        assertEquals("INVALID_CREDENTIALS", credentials.body?.properties?.get("code"))
        assertEquals("Email or password is invalid", credentials.body?.detail)
        val duplicate = handler.emailAlreadyRegistered(EmailAlreadyRegisteredException())
        checkContract(requireNotNull(duplicate.body), 409, "EMAIL_ALREADY_REGISTERED")
    }

    @Test
    fun `account linking preserves code and email needed by existing clients`() {
        val response = handler.accountLinkRequired(AccountLinkRequiredException("user@example.com"))
        checkContract(requireNotNull(response.body), 409, "ACCOUNT_LINK_REQUIRED")
        assertEquals("ACCOUNT_LINK_REQUIRED", response.body?.properties?.get("code"))
        assertEquals("user@example.com", response.body?.properties?.get("email"))
    }

    @Test
    fun `mobile authorization preserves the control code and no-store header`() {
        val response = MobileExceptionHandler(factory).handle(MobileFlowException("TRANSACTION_EXPIRED"), request)
        checkContract(requireNotNull(response.body), 400, "TRANSACTION_EXPIRED")
        assertEquals("TRANSACTION_EXPIRED", response.body?.properties?.get("code"))
        assertEquals("no-store", response.headers.cacheControl)
    }

    @Test
    fun `security and MVC use the same access denied code`() {
        val mapper = Mockito.mock(ObjectMapper::class.java)
        Mockito.`when`(mapper.writeValueAsString(Mockito.any(ProblemDetail::class.java))).thenReturn("{}")
        val response = MockHttpServletResponse()
        ProblemDetailAccessDeniedHandler(factory, mapper).handle(request, response, AccessDeniedException("Denied"))
        val captured = ArgumentCaptor.forClass(ProblemDetail::class.java)
        Mockito.verify(mapper).writeValueAsString(captured.capture())
        checkContract(captured.value, 403, "ACCESS_DENIED")
        assertEquals(403, response.status)
        assertTrue(response.contentType.orEmpty().startsWith("application/problem+json"))
        checkContract(requireNotNull(handler.accessDenied(AccessDeniedException("Denied")).body), 403, "ACCESS_DENIED")
    }

    @Test
    fun `security authentication errors are distinct from invalid login credentials`() {
        val mapper = Mockito.mock(ObjectMapper::class.java)
        Mockito.`when`(mapper.writeValueAsString(Mockito.any(ProblemDetail::class.java))).thenReturn("{}")
        val response = MockHttpServletResponse()
        ProblemDetailAuthenticationEntryPoint(factory, mapper).commence(request, response, BadCredentialsException("Token"))
        val captured = ArgumentCaptor.forClass(ProblemDetail::class.java)
        Mockito.verify(mapper).writeValueAsString(captured.capture())
        checkContract(captured.value, 401, "UNAUTHENTICATED")
        assertEquals(401, response.status)
    }

    @Test
    fun `unexpected errors do not expose the internal diagnostic`() {
        val response = handler.unhandled(IllegalStateException("secret internal diagnostic"))
        checkContract(requireNotNull(response.body), 500, "INTERNAL_ERROR")
        assertEquals("Ocorreu um erro interno inesperado. Se persistir, contate o suporte.", response.body?.detail)
        val notFound = handler.unhandled(ResponseStatusException(HttpStatus.NOT_FOUND))
        checkContract(requireNotNull(notFound.body), 404, "REQUEST_ERROR")
    }
}
