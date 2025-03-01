package top.sunbath.api.auth.service

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import top.sunbath.api.auth.BaseTest
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import java.util.UUID

@MicronautTest(environments = ["test"])
class AuthServiceTest : BaseTest() {
    @Inject
    lateinit var authService: AuthService

    // 生成唯一用户名的辅助函数
    private fun uniqueUsername(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}"

    @Test
    fun `test successful registration`() {
        // Given
        val request =
            CreateUserRequest(
                username = uniqueUsername("testuser"),
                email = "test@example.com",
                password = "Password123",
                fullName = "Test User",
            )

        // When
        val userId = authService.register(request)

        // Then
        assertNotNull(userId)
    }

    @Test
    fun `test registration with existing username`() {
        // Given
        val username = uniqueUsername("existinguser")
        val request =
            CreateUserRequest(
                username = username,
                email = "existing@example.com",
                password = "Password123",
                fullName = "Existing User",
            )
        authService.register(request)

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.register(request)
            }
        assertEquals(HttpStatus.CONFLICT, exception.status)
        assertEquals("Username already exists", exception.message)
    }

    @Test
    fun `test successful login`() {
        // Given
        val username = uniqueUsername("loginuser")
        val password = "Password123"
        val registerRequest =
            CreateUserRequest(
                username = username,
                email = "login@example.com",
                password = password,
                fullName = "Login User",
            )
        authService.register(registerRequest)

        // When
        val token = authService.login(LoginRequest(username, password))

        // Then
        assertNotNull(token)
    }

    @Test
    fun `test login with invalid username`() {
        // Given
        val request =
            LoginRequest(
                username = uniqueUsername("nonexistent"),
                password = "Password123",
            )

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.login(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `test login with invalid password`() {
        // Given
        val username = uniqueUsername("passworduser")
        val registerRequest =
            CreateUserRequest(
                username = username,
                email = "password@example.com",
                password = "Password123",
                fullName = "Password User",
            )
        authService.register(registerRequest)

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.login(LoginRequest(username, "wrongpassword"))
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid credentials", exception.message)
    }
}
