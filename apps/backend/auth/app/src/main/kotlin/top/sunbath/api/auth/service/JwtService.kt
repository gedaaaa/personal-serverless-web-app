package top.sunbath.api.auth.service

import io.micronaut.security.token.jwt.generator.JwtTokenGenerator
import jakarta.inject.Singleton
import top.sunbath.api.auth.model.User

/**
 * Service for JWT token operations.
 */
@Singleton
class JwtService(
    private val jwtTokenGenerator: JwtTokenGenerator,
) {
    /**
     * Generate a JWT token for a user.
     * @param user The user to generate token for
     * @return The generated JWT token
     */
    fun generateToken(user: User): String {
        val claims =
            mapOf(
                "sub" to user.id,
                "username" to user.username,
                "roles" to user.roles,
            )
        return jwtTokenGenerator.generateToken(claims).get()
    }
}
