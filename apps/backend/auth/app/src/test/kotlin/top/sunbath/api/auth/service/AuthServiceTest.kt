package top.sunbath.api.auth.service

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import top.sunbath.api.auth.model.User
import top.sunbath.api.auth.repository.UserRepository
import java.util.UUID

/**
 * Unit tests for the AuthService.
 */
@ExtendWith(MockKExtension::class)
class AuthServiceTest {
    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var jwtService: JwtService

    private lateinit var authService: AuthService

    // 生成唯一用户名的辅助函数
    private fun uniqueUsername(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}"

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        // 创建一个 spy 的 AuthService，这样我们可以模拟部分方法
        authService =
            spyk(
                AuthService(userRepository, jwtService),
                recordPrivateCalls = true,
            )
    }

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
        val hashedPassword = "hashed_password"
        val userId = "generated_user_id"

        // 模拟 AuthService 内部对 BCrypt 的调用
        every {
            authService["hashPassword"](request.password)
        } returns hashedPassword

        every { userRepository.findByUsername(request.username) } returns null
        every {
            userRepository.save(
                request.username,
                request.email,
                hashedPassword,
                setOf("ROLE_USER"),
                request.fullName,
            )
        } returns userId

        // When
        val result = authService.register(request)

        // Then
        assertNotNull(result)
        assertEquals(userId, result)

        verify(exactly = 1) { userRepository.findByUsername(request.username) }
        verify(exactly = 1) {
            userRepository.save(
                request.username,
                request.email,
                hashedPassword,
                setOf("ROLE_USER"),
                request.fullName,
            )
        }
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
        val existingUser =
            User(
                id = "existing_id",
                username = username,
                email = "existing@example.com",
                password = "hashed_password",
            )

        every { userRepository.findByUsername(username) } returns existingUser

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.register(request)
            }
        assertEquals(HttpStatus.CONFLICT, exception.status)
        assertEquals("Username already exists", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(username) }
    }

    @Test
    fun `test successful login`() {
        // Given
        val username = uniqueUsername("loginuser")
        val password = "Password123"
        val hashedPassword = "hashed_password"
        val user =
            User(
                id = "user_id",
                username = username,
                email = "login@example.com",
                password = hashedPassword,
            )
        val token = "jwt.token.example"
        val request = LoginRequest(username, password)

        // 模拟 AuthService 内部对 BCrypt 的调用
        every {
            authService["verifyPassword"](password, hashedPassword)
        } returns true

        every { userRepository.findByUsername(username) } returns user
        every { jwtService.generateToken(user) } returns token

        // When
        val result = authService.login(request)

        // Then
        assertNotNull(result)
        assertEquals(token, result)

        verify(exactly = 1) { userRepository.findByUsername(username) }
        verify(exactly = 1) { jwtService.generateToken(user) }
    }

    @Test
    fun `test login with invalid username`() {
        // Given
        val username = uniqueUsername("nonexistent")
        val request = LoginRequest(username, "Password123")

        every { userRepository.findByUsername(username) } returns null

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.login(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid credentials", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(username) }
    }

    @Test
    fun `test login with invalid password`() {
        // Given
        val username = uniqueUsername("passworduser")
        val password = "WrongPassword"
        val hashedPassword = "hashed_password"
        val user =
            User(
                id = "user_id",
                username = username,
                email = "password@example.com",
                password = hashedPassword,
            )
        val request = LoginRequest(username, password)

        // 模拟 AuthService 内部对 BCrypt 的调用
        every {
            authService["verifyPassword"](password, hashedPassword)
        } returns false

        every { userRepository.findByUsername(username) } returns user

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.login(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid credentials", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(username) }
    }
} 
