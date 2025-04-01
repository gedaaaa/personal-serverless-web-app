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
import top.sunbath.api.auth.service.email.EmailService
import java.time.Instant
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

    @MockK
    private lateinit var emailService: EmailService

    private lateinit var authService: AuthService

    // 生成唯一用户名的辅助函数
    private fun uniqueUsername(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}"

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        // 创建一个 spy 的 AuthService，这样我们可以模拟部分方法
        authService =
            spyk(
                AuthService(userRepository, jwtService, emailService),
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
        every { userRepository.findByEmail(request.email) } returns null
        every {
            userRepository.save(
                username = request.username,
                email = request.email,
                password = any(),
                roles = setOf("ROLE_USER"),
                fullName = request.fullName,
                emailVerified = false,
                emailVerificationToken = any(),
                emailVerificationTokenExpiresAt = any(),
                lastVerificationEmailSentAt = any(),
            )
        } returns userId

        every {
            emailService.sendVerificationEmail(
                to = request.email,
                username = request.username,
                verificationToken = any(),
                expiresAt = any(),
            )
        } returns Unit

        // When
        val result = authService.register(request)

        // Then
        assertNotNull(result)
        assertEquals(userId, result.userId)
        assertEquals("Registration successful. Please check your email for verification instructions.", result.message)

        verify(exactly = 1) { userRepository.findByUsername(request.username) }
        verify(exactly = 1) { userRepository.findByEmail(request.email) }
        verify(exactly = 1) {
            userRepository.save(
                username = request.username,
                email = request.email,
                password = any(),
                roles = setOf("ROLE_USER"),
                fullName = request.fullName,
                emailVerified = false,
                emailVerificationToken = any(),
                emailVerificationTokenExpiresAt = any(),
                lastVerificationEmailSentAt = any(),
            )
        }
        verify(exactly = 1) {
            emailService.sendVerificationEmail(
                to = request.email,
                username = request.username,
                verificationToken = any(),
                expiresAt = any(),
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
        verify(exactly = 0) { userRepository.findByEmail(any()) }
    }

    @Test
    fun `test registration with existing email`() {
        // Given
        val request =
            CreateUserRequest(
                username = uniqueUsername("newuser"),
                email = "existing@example.com",
                password = "Password123",
                fullName = "New User",
            )
        val existingUser =
            User(
                id = "existing_id",
                username = "existinguser",
                email = request.email,
                password = "hashed_password",
            )

        every { userRepository.findByUsername(request.username) } returns null
        every { userRepository.findByEmail(request.email) } returns existingUser

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.register(request)
            }
        assertEquals(HttpStatus.CONFLICT, exception.status)
        assertEquals("Email already exists", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(request.username) }
        verify(exactly = 1) { userRepository.findByEmail(request.email) }
    }

    @Test
    fun `test successful login with verified email`() {
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
                emailVerified = true,
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
    fun `test login with unverified email`() {
        // Given
        val username = uniqueUsername("unverifieduser")
        val password = "Password123"
        val hashedPassword = "hashed_password"
        val user =
            User(
                id = "user_id",
                username = username,
                email = "unverified@example.com",
                password = hashedPassword,
                emailVerified = false,
            )
        val request = LoginRequest(username, password)

        // 模拟 AuthService 内部对 BCrypt 的调用
        every {
            authService["verifyPassword"](password, hashedPassword)
        } returns true

        every { userRepository.findByUsername(username) } returns user

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.login(request)
            }
        assertEquals(HttpStatus.FORBIDDEN, exception.status)
        assertEquals("Email not verified. Please check your email for verification instructions.", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(username) }
        verify(exactly = 0) { jwtService.generateToken(any()) }
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
                emailVerified = true,
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
        verify(exactly = 0) { jwtService.generateToken(any()) }
    }

    @Test
    fun `test verify email with valid token`() {
        // Given
        val token = "valid_token"
        val user =
            User(
                id = "user_id",
                username = uniqueUsername("testuser"),
                email = "test@example.com",
                password = "hashed_password",
                emailVerified = false,
                emailVerificationToken = token,
                emailVerificationTokenExpiresAt = Instant.now().plusSeconds(3600),
            )

        every { userRepository.findByVerificationToken(token) } returns user
        every {
            userRepository.update(
                id = user.id!!,
                email = user.email,
                password = user.password,
                roles = user.roles,
                fullName = user.fullName,
                emailVerified = true,
                emailVerificationToken = null,
                emailVerificationTokenExpiresAt = null,
                lastVerificationEmailSentAt = user.lastVerificationEmailSentAt,
            )
        } returns true

        // When
        val result = authService.verifyEmail(token)

        // Then
        assertEquals("Email verified successfully", result)
        verify(exactly = 1) { userRepository.findByVerificationToken(token) }
        verify(exactly = 1) {
            userRepository.update(
                id = user.id!!,
                email = user.email,
                password = user.password,
                roles = user.roles,
                fullName = user.fullName,
                emailVerified = true,
                emailVerificationToken = null,
                emailVerificationTokenExpiresAt = null,
                lastVerificationEmailSentAt = user.lastVerificationEmailSentAt,
            )
        }
    }

    @Test
    fun `test verify email with invalid token`() {
        // Given
        val token = "invalid_token"

        every { userRepository.findByVerificationToken(token) } returns null

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.verifyEmail(token)
            }
        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("Invalid verification token", exception.message)

        verify(exactly = 1) { userRepository.findByVerificationToken(token) }
        verify(exactly = 0) { userRepository.update(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test verify email with expired token`() {
        // Given
        val token = "expired_token"
        val user =
            User(
                id = "user_id",
                username = uniqueUsername("testuser"),
                email = "test@example.com",
                password = "hashed_password",
                emailVerified = false,
                emailVerificationToken = token,
                emailVerificationTokenExpiresAt = Instant.now().minusSeconds(3600),
            )

        every { userRepository.findByVerificationToken(token) } returns user

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.verifyEmail(token)
            }
        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("Verification token has expired", exception.message)

        verify(exactly = 1) { userRepository.findByVerificationToken(token) }
        verify(exactly = 0) { userRepository.update(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test resend verification email successfully`() {
        // Given
        val email = "test@example.com"
        val user =
            User(
                id = "user_id",
                username = uniqueUsername("testuser"),
                email = email,
                password = "hashed_password",
                emailVerified = false,
                lastVerificationEmailSentAt = Instant.now().minusSeconds(3600),
            )

        every { userRepository.findByEmail(email) } returns user
        every {
            userRepository.update(
                id = user.id!!,
                email = user.email,
                password = user.password,
                roles = user.roles,
                fullName = user.fullName,
                emailVerified = user.emailVerified,
                emailVerificationToken = any(),
                emailVerificationTokenExpiresAt = any(),
                lastVerificationEmailSentAt = any(),
            )
        } returns true
        every {
            emailService.sendVerificationEmail(
                to = email,
                username = user.username!!,
                verificationToken = any(),
                expiresAt = any(),
            )
        } returns Unit

        // When
        val result = authService.resendVerificationEmail(email)

        // Then
        assertEquals("Verification email sent successfully", result)
        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 1) {
            userRepository.update(
                id = user.id!!,
                email = user.email,
                password = user.password,
                roles = user.roles,
                fullName = user.fullName,
                emailVerified = user.emailVerified,
                emailVerificationToken = any(),
                emailVerificationTokenExpiresAt = any(),
                lastVerificationEmailSentAt = any(),
            )
        }
        verify(exactly = 1) {
            emailService.sendVerificationEmail(
                to = email,
                username = user.username!!,
                verificationToken = any(),
                expiresAt = any(),
            )
        }
    }

    @Test
    fun `test resend verification email to non-existent email`() {
        // Given
        val email = "nonexistent@example.com"

        every { userRepository.findByEmail(email) } returns null

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.resendVerificationEmail(email)
            }
        assertEquals(HttpStatus.NOT_FOUND, exception.status)
        assertEquals("Email not found", exception.message)

        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 0) { userRepository.update(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any(), any(), any()) }
    }

    @Test
    fun `test resend verification email to already verified email`() {
        // Given
        val email = "verified@example.com"
        val user =
            User(
                id = "user_id",
                username = uniqueUsername("testuser"),
                email = email,
                password = "hashed_password",
                emailVerified = true,
            )

        every { userRepository.findByEmail(email) } returns user

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.resendVerificationEmail(email)
            }
        assertEquals(HttpStatus.BAD_REQUEST, exception.status)
        assertEquals("Email is already verified", exception.message)

        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 0) { userRepository.update(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any(), any(), any()) }
    }

    @Test
    fun `test resend verification email too frequently`() {
        // Given
        val email = "test@example.com"
        val user =
            User(
                id = "user_id",
                username = uniqueUsername("testuser"),
                email = email,
                password = "hashed_password",
                emailVerified = false,
                lastVerificationEmailSentAt = Instant.now().minusSeconds(60), // 1 minute ago
            )

        every { userRepository.findByEmail(email) } returns user

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.resendVerificationEmail(email)
            }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.status)
        assertEquals("Please wait before requesting another verification email", exception.message)

        verify(exactly = 1) { userRepository.findByEmail(email) }
        verify(exactly = 0) { userRepository.update(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { emailService.sendVerificationEmail(any(), any(), any(), any()) }
    }
}
