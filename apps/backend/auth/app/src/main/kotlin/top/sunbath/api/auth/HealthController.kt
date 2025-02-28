package top.sunbath.api.auth

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import java.time.LocalDateTime

/**
 * Health check controller for the auth service.
 * Provides endpoints to verify the service is running correctly.
 */
@Controller("/health")
open class HealthController {
    /**
     * Simple health check endpoint that returns the current status and timestamp.
     * @return A map containing status information and current server time.
     */
    @Get
    fun check() =
        mapOf(
            "status" to "UP",
            "service" to "auth",
            "timestamp" to LocalDateTime.now().toString(),
        )
}
