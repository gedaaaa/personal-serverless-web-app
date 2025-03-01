package top.sunbath.api.auth.service

import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.validator.TokenValidator
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import top.sunbath.api.auth.BaseTest
import top.sunbath.api.auth.model.User

@MicronautTest(environments = ["test"])
class JwtServiceTest : BaseTest() {
    @Inject
    lateinit var jwtService: JwtService

    @Inject
    lateinit var tokenValidator: TokenValidator<Authentication>

    @Test
    fun `test generate token`() {
        // Given
        val user =
            User(
                id = "test-id",
                username = "testuser",
                email = "test@example.com",
                password = "hashedpassword",
                roles = setOf("ROLE_USER"),
                fullName = "Test User",
            )

        // When
        val token = jwtService.generateToken(user)

        // Then
        assertNotNull(token)

        // Validate token
        val authentication = Mono.from(tokenValidator.validateToken(token, null)).block()
        assertNotNull(authentication)
        assertEquals(user.id, authentication?.attributes?.get("sub"))
        assertEquals(user.username, authentication?.attributes?.get("username"))

        val tokenRoles = authentication?.attributes?.get("roles") as Collection<*>
        assertTrue(tokenRoles.containsAll(user.roles))
        assertTrue(user.roles.containsAll(tokenRoles))
    }
}
