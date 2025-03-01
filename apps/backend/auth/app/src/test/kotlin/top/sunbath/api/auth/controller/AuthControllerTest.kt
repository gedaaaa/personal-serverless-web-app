package top.sunbath.api.auth.controller

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest

@MicronautTest(environments = ["test"])
class AuthControllerTest {
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun `test successful registration`() {
        // Given
        val request =
            CreateUserRequest(
                username = "testuser",
                email = "test@example.com",
                password = "Password123",
                fullName = "Test User",
            )

        // When
        val response =
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/register", request),
                Map::class.java,
            )

        // Then
        assertEquals(HttpStatus.CREATED, response.status)
        assertNotNull(response.body()?.get("id"))
    }

    @Test
    fun `test registration with invalid email`() {
        // Given
        val request =
            CreateUserRequest(
                username = "testuser",
                email = "invalid-email",
                password = "Password123",
                fullName = "Test User",
            )

        // When/Then
        assertThrows<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/register", request),
                Map::class.java,
            )
        }
    }

    @Test
    fun `test registration with weak password`() {
        // Given
        val request =
            CreateUserRequest(
                username = "testuser",
                email = "test@example.com",
                password = "weak",
                fullName = "Test User",
            )

        // When/Then
        assertThrows<HttpClientResponseException> {
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/register", request),
                Map::class.java,
            )
        }
    }

    @Test
    fun `test successful login`() {
        // Given
        val username = "loginuser"
        val password = "Password123"
        val registerRequest =
            CreateUserRequest(
                username = username,
                email = "login@example.com",
                password = password,
                fullName = "Login User",
            )
        client.toBlocking().exchange(
            HttpRequest.POST("/auth/register", registerRequest),
            Map::class.java,
        )

        // When
        val loginRequest = LoginRequest(username, password)
        val response =
            client.toBlocking().exchange(
                HttpRequest.POST("/auth/login", loginRequest),
                Map::class.java,
            )

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertNotNull(response.body()?.get("token"))
    }

    @Test
    fun `test login with invalid credentials`() {
        // Given
        val request = LoginRequest("nonexistent", "wrongpassword")

        // When/Then
        val exception =
            assertThrows<HttpClientResponseException> {
                client.toBlocking().exchange(
                    HttpRequest.POST("/auth/login", request),
                    Map::class.java,
                )
            }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }
}
