package top.sunbath.api.auth.controller

import io.mockk.MockKAnnotations
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.auth.HealthController

/**
 * Unit tests for the HealthController.
 */
@ExtendWith(MockKExtension::class)
class HealthControllerTest {
    private lateinit var controller: HealthController

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        controller = HealthController()
    }

    @Test
    fun `test health check returns correct status`() {
        // When
        val result = controller.check()

        // Then
        assertNotNull(result)
        assertEquals("UP", result["status"])
        assertEquals("auth", result["service"])
        assertNotNull(result["timestamp"])
    }
} 
