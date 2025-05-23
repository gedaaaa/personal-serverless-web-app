package top.sunbath.api.auth.controller

import io.micronaut.http.HttpStatus
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import top.sunbath.api.auth.controller.response.RegisterResponse
import top.sunbath.api.auth.service.AuthService
import top.sunbath.api.auth.service.outcome.LoginOutcome
import java.util.UUID

/**
 * Tests for AuthController.
 */
@ExtendWith(MockKExtension::class)
class AuthControllerTest {
    @MockK
    private lateinit var authService: AuthService

    @InjectMockKs
    private lateinit var controller: AuthController

    // 生成唯一用户名的辅助函数
    private fun uniqueUsername(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}"

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `test successful registration`() {
        // Given
        val username = uniqueUsername("registeruser")
        val request =
            CreateUserRequest(
                username = username,
                email = "test@example.com",
                password = "Password123",
                fullName = "Test User",
            )
        val userId = UUID.randomUUID().toString()
        val expectedResponse = RegisterResponse(userId, "User registered successfully")

        every { authService.register(request) } returns expectedResponse

        // When
        val response = controller.register(request)

        // Then
        assertEquals(HttpStatus.CREATED, response.status)
        val body = response.body()
        assertNotNull(body)
        assertEquals(expectedResponse.userId, body.userId)
        assertEquals(expectedResponse.message, body.message)

        verify(exactly = 1) { authService.register(request) }
    }

    @Test
    fun `test successful login`() {
        // Given
        val username = uniqueUsername("loginuser")
        val password = "Password123"
        val loginRequest = LoginRequest(username, password)
        val token = "jwt.token.example"

        every { authService.login(loginRequest) } returns LoginOutcome.Success(token)

        // When
        val response = controller.login(loginRequest)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        val body = response.body()
        assertNotNull(body)
        assertTrue(body is LoginOutcome.Success)
        assertEquals(token, (body as LoginOutcome.Success).token)

        verify(exactly = 1) { authService.login(loginRequest) }
    }
}
