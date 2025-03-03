package top.sunbath.api.auth.service

import io.micronaut.security.token.jwt.generator.JwtTokenGenerator
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.auth.model.User
import java.util.Optional

/**
 * Unit tests for the JwtService.
 */
@ExtendWith(MockKExtension::class)
class JwtServiceTest {
    @MockK
    private lateinit var jwtTokenGenerator: JwtTokenGenerator

    @InjectMockKs
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

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
        val expectedToken = "jwt.token.example"
        val claimsSlot = slot<Map<String, Any>>()

        every { jwtTokenGenerator.generateToken(capture(claimsSlot)) } returns Optional.of(expectedToken)

        // When
        val token = jwtService.generateToken(user)

        // Then
        assertNotNull(token)
        assertEquals(expectedToken, token)

        // Verify claims
        val claims = claimsSlot.captured
        assertEquals(user.id, claims["sub"])
        assertEquals(user.username, claims["username"])
        assertEquals(user.roles, claims["roles"])

        verify(exactly = 1) { jwtTokenGenerator.generateToken(any()) }
    }
} 
