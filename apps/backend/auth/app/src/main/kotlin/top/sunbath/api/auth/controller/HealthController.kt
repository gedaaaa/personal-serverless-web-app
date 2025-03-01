package top.sunbath.api.auth

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.time.LocalDateTime

/**
 * Health check controller for the auth service.
 * Provides endpoints to verify the service is running correctly.
 */
@Controller("/health")
@Secured(SecurityRule.IS_ANONYMOUS)
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
            "depolyment_trigger" to 1,
        )
}
