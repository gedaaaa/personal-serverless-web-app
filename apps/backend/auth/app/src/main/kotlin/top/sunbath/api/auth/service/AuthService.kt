package top.sunbath.api.auth.service

import at.favre.lib.crypto.bcrypt.BCrypt
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import top.sunbath.api.auth.repository.UserRepository

/**
 * Service for authentication operations.
 */
@Singleton
class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService,
) {
    companion object {
        private val BCRYPT_VERSION = BCrypt.Version.VERSION_2A
        private const val BCRYPT_COST = 12
    }

    /**
     * Register a new user.
     * @param request The registration request
     * @return The created user's ID
     */
    fun register(request: CreateUserRequest): String {
        // Check if username already exists
        userRepository.findByUsername(request.username)?.let {
            throw HttpStatusException(HttpStatus.CONFLICT, "Username already exists")
        }

        // Hash password
        val hashedPassword = hashPassword(request.password)

        // Create user
        return userRepository.save(
            request.username,
            request.email,
            hashedPassword,
            setOf("ROLE_USER"),
            request.fullName,
        )
    }

    /**
     * Authenticate a user and generate JWT token.
     * @param request The login request
     * @return The JWT token
     */
    fun login(request: LoginRequest): String {
        // Find user by username
        val user =
            userRepository.findByUsername(request.username)
                ?: throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")

        // Verify password
        if (!verifyPassword(request.password, user.password ?: "")) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        // Generate JWT token
        return jwtService.generateToken(user)
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
}
