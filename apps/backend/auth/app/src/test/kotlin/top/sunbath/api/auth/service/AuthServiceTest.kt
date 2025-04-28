package top.sunbath.api.auth.service

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import top.sunbath.api.auth.controller.request.MigratePasswordRequest
import top.sunbath.api.auth.model.PasswordType
import top.sunbath.api.auth.model.User
import top.sunbath.api.auth.repository.UserRepository
import top.sunbath.api.auth.service.email.EmailService
import top.sunbath.api.auth.service.outcome.LoginOutcome
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

    // Test Data Constants
    private val v1Username = uniqueUsername("v1user")
    private val v1UserId = "v1_user_id_${UUID.randomUUID().toString().substring(0, 4)}"
    private val v1FrontendHash = "v1_frontend_hashed_password"
    private val v1BackendHash = "\$2a\$12\$${UUID.randomUUID()}" // Placeholder for BCrypt hash
    private val v2FrontendHash = "v2_frontend_hashed_password"
    private val v2BackendHash = "\$2a\$12\$${UUID.randomUUID()}" // Placeholder for BCrypt hash
    private val validMigrationToken = "valid_migration_token_${UUID.randomUUID()}"
    private val invalidMigrationToken = "invalid_migration_token_${UUID.randomUUID()}"
    private val validJwtToken = "valid.jwt.token"

    // 生成唯一用户名的辅助函数
    private fun uniqueUsername(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}"

    // Helper to create V1 user mock
    private fun createV1UserMock(
        migrationToken: String? = null,
        migrationTokenExpiresAt: Instant? = null,
    ): User =
        spyk(
            User(
                id = v1UserId,
                username = v1Username,
                email = "$v1Username@example.com",
                password = v1BackendHash, // Store backend hash
                passwordType = PasswordType.V1,
                emailVerified = true,
                migrationToken = migrationToken,
                migrationTokenExpiresAt = migrationTokenExpiresAt,
            ),
            recordPrivateCalls = true,
        )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        // 创建一个 spy 的 AuthService，这样我们可以模拟部分方法
        authService =
            spyk(
                AuthService(userRepository, jwtService, emailService),
                recordPrivateCalls = true,
            )

        // Mock hashing and verification used internally by AuthService
        // We mock these on the spy object itself
        every { authService["hashPassword"](v1FrontendHash) } returns v1BackendHash // Needed if register uses it directly
        every { authService["hashPassword"](v2FrontendHash) } returns v2BackendHash
        every { authService["verifyPassword"](v1FrontendHash, v1BackendHash) } returns true
        every { authService["verifyPassword"](neq(v1FrontendHash), v1BackendHash) } returns false // For incorrect password test
        // Assume V2 verification works similarly if needed for other tests
        every { authService["verifyPassword"](v2FrontendHash, v2BackendHash) } returns true

        // Mock token generation used internally by AuthService
        every { authService["generateMigrationToken"]() } returns validMigrationToken
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
                passwordType = PasswordType.V2,
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
        assertTrue(result is LoginOutcome.Success)
        assertEquals(token, (result as LoginOutcome.Success).token)

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
                passwordType = PasswordType.V2,
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
        val password = "WrongPassword" // This is Frontend Hash for V2
        val hashedPassword = "hashed_password" // This is Backend Hash for V2
        val user =
            User(
                id = "user_id",
                username = username,
                email = "password@example.com",
                password = hashedPassword,
                passwordType = PasswordType.V2, // Explicitly V2
                emailVerified = true,
            )
        val request = LoginRequest(username, password)

        // Use the spy's internal method mock setup in @BeforeEach
        every { authService["verifyPassword"](password, hashedPassword) } returns false // Explicitly mock failure for THIS test case

        every { userRepository.findByUsername(username) } returns user

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.login(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid credentials", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(username) }
        // Verify internal call
        verify(exactly = 1) { authService["verifyPassword"](password, hashedPassword) }
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
                id = user.id,
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
                id = user.id,
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
                id = user.id,
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
                username = user.username,
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
                id = user.id,
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
                username = user.username,
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

    // --- V1 Password Migration Tests ---

    @Test
    fun `test login with v1 password triggers migration`() {
        // Given
        val user = createV1UserMock() // V1 user, no token initially
        val request = LoginRequest(v1Username, v1FrontendHash)
        val migrationTokenSlot = slot<String>()
        val expiresAtSlot = slot<Instant>()

        every { userRepository.findByUsername(v1Username) } returns user
        // Capture arguments passed to updatePasswordSettings
        every {
            userRepository.updatePasswordSettings(
                id = eq(v1UserId),
                password = null, // Expect null as per AuthService logic
                passwordType = null, // Expect null as per AuthService logic
                migrationToken = capture(migrationTokenSlot),
                migrationTokenExpiresAt = capture(expiresAtSlot),
            )
        } returns true

        // When
        val result = authService.login(request)

        // Then
        assertTrue(result is LoginOutcome.MigrationRequired)
        assertEquals(validMigrationToken, (result as LoginOutcome.MigrationRequired).migrationToken)

        verify(exactly = 1) { userRepository.findByUsername(v1Username) }
        verify(exactly = 1) { authService["generateMigrationToken"]() }
        verify(exactly = 1) {
            userRepository.updatePasswordSettings(
                v1UserId,
                null,
                null,
                validMigrationToken,
                expiresAtSlot.captured,
            )
        }
        assertNotNull(expiresAtSlot.captured)
    }

    @Test
    fun `test migrate password successful`() {
        // Given
        val user =
            createV1UserMock(
                migrationToken = validMigrationToken,
                migrationTokenExpiresAt = Instant.now().plusSeconds(60),
            )
        val request =
            MigratePasswordRequest(
                username = v1Username,
                originalPassword = v1FrontendHash,
                desiredPassword = v2FrontendHash,
                migrationToken = validMigrationToken,
            )
        val updatedPassword = v2BackendHash
        val updatedType = PasswordType.V2
        val clearedToken = null
        val clearedExpiresAt = null

        every { userRepository.findByUsername(v1Username) } returns user
        every {
            userRepository.updatePasswordSettings(
                id = eq(v1UserId),
                password = updatedPassword,
                passwordType = updatedType,
                migrationToken = clearedToken,
                migrationTokenExpiresAt = clearedExpiresAt,
            )
        } returns true
        every { jwtService.generateToken(user) } returns validJwtToken // Use the same user object

        // When
        val result = authService.migratePassword(request)

        // Then
        assertEquals(validJwtToken, result)

        verify(exactly = 1) { userRepository.findByUsername(v1Username) }
        verify(exactly = 1) { user.isMigrationTokenValid(validMigrationToken) }
        verify(exactly = 1) { authService["verifyPassword"](v1FrontendHash, v1BackendHash) }
        verify(exactly = 1) { authService["hashPassword"](v2FrontendHash) }
        verify(exactly = 1) {
            userRepository.updatePasswordSettings(
                v1UserId,
                updatedPassword,
                updatedType,
                clearedToken,
                clearedExpiresAt,
            )
        }

        verify(exactly = 1) { jwtService.generateToken(user) }
    }

    @Test
    fun `test migrate password with invalid token`() {
        // Given
        val user =
            createV1UserMock(
                migrationToken = validMigrationToken, // User has a valid token stored
                migrationTokenExpiresAt = Instant.now().plusSeconds(60),
            )
        // But the request uses an invalid one
        val request =
            MigratePasswordRequest(
                username = v1Username,
                originalPassword = v1FrontendHash,
                desiredPassword = v2FrontendHash,
                migrationToken = invalidMigrationToken,
            )

        every { userRepository.findByUsername(v1Username) } returns user
        // Mock that the user's validation fails for the invalid token
        every { user.isMigrationTokenValid(invalidMigrationToken) } returns false

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.migratePassword(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid or expired migration token", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(v1Username) }
        verify(exactly = 1) { user.isMigrationTokenValid(invalidMigrationToken) }
        verify(exactly = 0) { authService["verifyPassword"](any<String>(), any<String>()) }
        verify(exactly = 0) { userRepository.updatePasswordSettings(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test migrate password with expired token`() {
        // Given
        val user =
            createV1UserMock(
                migrationToken = validMigrationToken,
                migrationTokenExpiresAt = Instant.now().minusSeconds(60), // Token is expired
            )
        val request =
            MigratePasswordRequest(
                username = v1Username,
                originalPassword = v1FrontendHash,
                desiredPassword = v2FrontendHash,
                migrationToken = validMigrationToken,
            )

        every { userRepository.findByUsername(v1Username) } returns user
        // Mock that the user's validation fails (due to expiration check inside isMigrationTokenValid)
        every { user.isMigrationTokenValid(validMigrationToken) } returns false

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.migratePassword(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid or expired migration token", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(v1Username) }
        verify(exactly = 1) { user.isMigrationTokenValid(validMigrationToken) }
        verify(exactly = 0) { authService["verifyPassword"](any<String>(), any<String>()) }
        verify(exactly = 0) { userRepository.updatePasswordSettings(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test migrate password with incorrect original password`() {
        // Given
        val incorrectFrontendHash = "incorrect_" + v1FrontendHash
        val user =
            createV1UserMock(
                migrationToken = validMigrationToken,
                migrationTokenExpiresAt = Instant.now().plusSeconds(60),
            )
        val request =
            MigratePasswordRequest(
                username = v1Username,
                originalPassword = incorrectFrontendHash,
                desiredPassword = v2FrontendHash,
                migrationToken = validMigrationToken,
            )

        every { userRepository.findByUsername(v1Username) } returns user
        every { user.isMigrationTokenValid(validMigrationToken) } returns true
        // verifyPassword for incorrect hash returns false (mocked in setup)

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.migratePassword(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid original password", exception.message)

        verify(exactly = 1) { userRepository.findByUsername(v1Username) }
        verify(exactly = 1) { user.isMigrationTokenValid(validMigrationToken) }
        verify(exactly = 1) { authService["verifyPassword"](incorrectFrontendHash, v1BackendHash) } // verify attempted
        verify(exactly = 0) { userRepository.updatePasswordSettings(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test migrate password for non existent user`() {
        // Given
        val nonExistentUsername = "non_existent_user"
        val request = MigratePasswordRequest(nonExistentUsername, validMigrationToken, v1FrontendHash, v2FrontendHash)

        every { userRepository.findByUsername(nonExistentUsername) } returns null

        // When/Then
        val exception =
            assertThrows<HttpStatusException> {
                authService.migratePassword(request)
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
        assertEquals("Invalid credentials", exception.message) // This message comes from findByUsername failing

        verify(exactly = 1) { userRepository.findByUsername(nonExistentUsername) }
        verify(exactly = 0) { authService["verifyPassword"](any<String>(), any<String>()) }
        verify(exactly = 0) { userRepository.updatePasswordSettings(any(), any(), any(), any(), any()) }
    }
}
