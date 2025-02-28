package top.sunbath.api.auth

import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@MicronautTest
class HealthControllerTest {
    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Test
    fun testHealthEndpoint() {
        val response = client.toBlocking().retrieve("/health", Map::class.java) as Map<String, Any>

        assertEquals("UP", response["status"])
        assertEquals("auth", response["service"])
        assertNotNull(response["timestamp"])
    }
}
