package top.sunbath.api.auth.service

import at.favre.lib.crypto.bcrypt.BCrypt
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import top.sunbath.api.auth.controller.response.RegisterResponse
import top.sunbath.api.auth.repository.UserRepository
import top.sunbath.api.auth.service.email.EmailService
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

/**
 * Service for authentication operations.
 */
@Singleton
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
    private val emailService: EmailService,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    companion object {
        private val BCRYPT_VERSION = BCrypt.Version.VERSION_2A
        private const val BCRYPT_COST = 12
        private const val VERIFICATION_TOKEN_LENGTH = 32
        private const val VERIFICATION_TOKEN_EXPIRES_IN_HOURS = 24L
        private const val MIN_VERIFICATION_EMAIL_INTERVAL_SECONDS = 300L // 5 minutes
        private val secureRandom = SecureRandom()
    }

    /**
     * Register a new user.
     * @param request The registration request
     * @return The registration response
     */
    fun register(request: CreateUserRequest): RegisterResponse {
        // Check if username already exists
        userRepository.findByUsername(request.username)?.let {
            throw HttpStatusException(HttpStatus.CONFLICT, "Username already exists")
        }

        // Check if email already exists
        userRepository.findByEmail(request.email)?.let {
            throw HttpStatusException(HttpStatus.CONFLICT, "Email already exists")
        }

        // Generate verification token
        val verificationToken = generateVerificationToken()
        val verificationTokenExpiresAt = Instant.now().plusSeconds(VERIFICATION_TOKEN_EXPIRES_IN_HOURS * 3600)

        // Hash password
        val hashedPassword = hashPassword(request.password)

        // Create user
        val userId =
            userRepository.save(
                username = request.username,
                email = request.email,
                password = hashedPassword,
                roles = setOf("ROLE_USER"),
                fullName = request.fullName,
                emailVerified = false,
                emailVerificationToken = verificationToken,
                emailVerificationTokenExpiresAt = verificationTokenExpiresAt,
                lastVerificationEmailSentAt = Instant.now(),
            )

        // Send verification email
        try {
            emailService.sendVerificationEmail(
                to = request.email,
                username = request.username,
                verificationToken = verificationToken,
                expiresAt = verificationTokenExpiresAt,
            )
        } catch (e: Exception) {
            // Log error but don't fail registration
            logger.error("Failed to send verification email", e)
        }

        return RegisterResponse(
            userId = userId,
            message = "Registration successful. Please check your email for verification instructions.",
        )
    }

    /**
     * Authenticate a user and generate JWT token.
     * @param request The login request
     * @return The JWT token
     * @throws HttpStatusException if the credentials are invalid or email is not verified
     */
    fun login(request: LoginRequest): String {
        // Find user by username
        val user =
            userRepository.findByUsername(request.username)
                ?: throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")

        // Verify password
        if (!verifyPassword(request.password, user.password)) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        // Check email verification
        if (!user.emailVerified) {
            throw HttpStatusException(
                HttpStatus.FORBIDDEN,
                "Email not verified. Please check your email for verification instructions.",
            )
        }

        // Generate JWT token
        return jwtService.generateToken(user)
    }

    /**
     * Generate a random verification token.
     * @return The verification token
     */
    private fun generateVerificationToken(): String {
        val bytes = ByteArray(VERIFICATION_TOKEN_LENGTH)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Hash a password using BCrypt.
     * @param password The password to hash
     * @return The hashed password
     */
    private fun hashPassword(password: String): String = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())

    /**
     * Verify a password against a hashed password.
     * @param password The password to verify
     * @param hashedPassword The hashed password to verify against
     * @return True if the password matches the hashed password, false otherwise
     */
    private fun verifyPassword(
        password: String,
        hashedPassword: String,
    ): Boolean {
        val result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword.toCharArray())
        return result.verified
    }

    /**
     * Verify email with the provided token.
     * @param token The verification token
     * @return A message describing the result
     * @throws HttpStatusException if the token is invalid or expired
     */
    fun verifyEmail(token: String): String {
        // Find user by verification token
        val user =
            userRepository.findByVerificationToken(token)
                ?: throw HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid verification token")

        // Check if token is valid and not expired
        if (!user.isVerificationTokenValid()) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Verification token has expired")
        }

        // Mark email as verified
        user.markEmailAsVerified()
        userRepository.update(
            id = user.id,
            email = user.email,
            password = user.password,
            roles = user.roles,
            fullName = user.fullName,
            emailVerified = user.emailVerified,
            emailVerificationToken = user.emailVerificationToken,
            emailVerificationTokenExpiresAt = user.emailVerificationTokenExpiresAt,
            lastVerificationEmailSentAt = user.lastVerificationEmailSentAt,
        )

        return "Email verified successfully"
    }

    /**
     * Resend verification email to the specified email address.
     * @param email The email address
     * @return A message describing the result
     * @throws HttpStatusException if the email is not found or already verified
     */
    fun resendVerificationEmail(email: String): String {
        // Find user by email
        val user =
            userRepository.findByEmail(email)
                ?: throw HttpStatusException(HttpStatus.NOT_FOUND, "Email not found")

        // Check if email is already verified
        if (user.emailVerified) {
            throw HttpStatusException(HttpStatus.BAD_REQUEST, "Email is already verified")
        }

        // Check if we can send another verification email
        if (!user.canSendVerificationEmail(MIN_VERIFICATION_EMAIL_INTERVAL_SECONDS)) {
            throw HttpStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Please wait before requesting another verification email",
            )
        }

        // Generate new verification token
        val verificationToken = generateVerificationToken()
        val verificationTokenExpiresAt = Instant.now().plusSeconds(VERIFICATION_TOKEN_EXPIRES_IN_HOURS * 3600)

        // Update user with new token
        user.updateVerificationToken(verificationToken, VERIFICATION_TOKEN_EXPIRES_IN_HOURS * 3600)
        userRepository.update(
            id = user.id,
            email = user.email,
            password = user.password,
            roles = user.roles,
            fullName = user.fullName,
            emailVerified = user.emailVerified,
            emailVerificationToken = user.emailVerificationToken,
            emailVerificationTokenExpiresAt = user.emailVerificationTokenExpiresAt,
            lastVerificationEmailSentAt = user.lastVerificationEmailSentAt,
        )

        // Send verification email
        try {
            emailService.sendVerificationEmail(
                to = email,
                username = user.username!!,
                verificationToken = verificationToken,
                expiresAt = verificationTokenExpiresAt,
            )
        } catch (e: Exception) {
            // Log error but don't fail the operation
            logger.error("Failed to send verification email", e)
            throw HttpStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to send verification email. Please try again later.",
            )
        }

        return "Verification email sent successfully"
    }
}
