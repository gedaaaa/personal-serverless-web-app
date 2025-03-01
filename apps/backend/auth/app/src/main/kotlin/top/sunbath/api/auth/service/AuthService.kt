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
        val hashedPassword = BCrypt.withDefaults().hashToString(BCRYPT_COST, request.password.toCharArray())

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
        val result = BCrypt.verifyer().verify(request.password.toCharArray(), user.password?.toCharArray())
        if (!result.verified) {
            throw HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        }

        // Generate JWT token
        return jwtService.generateToken(user)
    }
}
