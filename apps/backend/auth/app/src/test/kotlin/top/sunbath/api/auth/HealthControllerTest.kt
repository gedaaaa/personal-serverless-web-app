package top.sunbath.api.auth

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
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
        val response =
            client.toBlocking().exchange(
                HttpRequest.GET<Map<String, Any>>("/health"),
                Map::class.java,
            )

        assertEquals(HttpStatus.OK, response.status)
        val body = response.body()
        assertEquals("UP", body["status"])
        assertEquals("auth", body["service"])
        assertNotNull(body["timestamp"])
    }
}
