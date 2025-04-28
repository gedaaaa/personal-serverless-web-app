package top.sunbath.api.auth.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import top.sunbath.api.auth.controller.request.MigratePasswordRequest
import top.sunbath.api.auth.controller.response.RegisterResponse
import top.sunbath.api.auth.service.AuthService
import top.sunbath.api.auth.service.outcome.LoginOutcome

/**
 * Controller for authentication operations.
 */
@Validated
@Controller("/v1")
@Secured(SecurityRule.IS_ANONYMOUS)
class AuthController(
    private val authService: AuthService,
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    /**
     * Register a new user.
     * @param request The registration request
     * @return HTTP response with registration details
     */
    @Post("/register")
    fun register(
        @Body @Valid request: CreateUserRequest,
    ): HttpResponse<RegisterResponse> {
        val response = authService.register(request)
        return HttpResponse.created(response)
    }

    /**
     * Login with username and password.
     * @param request The login request
     * @return The JWT token
     */
    @Post("/login")
    fun login(
        @Body @Valid request: LoginRequest,
    ): HttpResponse<LoginOutcome> {
        val outcome = authService.login(request)
        if (outcome is LoginOutcome.MigrationRequired) {
            logger.error("Migration required for user ${request.username}")
            logger.error(outcome.toString())
            return HttpResponse
                .status<LoginOutcome>(HttpStatus.FORBIDDEN)
                .body(outcome)
        }
        return HttpResponse.ok(outcome)
    }

    /**
     * Migrate password from plain text to SHA-256.
     * @param request The migration request
     * @return The JWT token
     */
    @Post("/migrate-password")
    fun migratePassword(
        @Body @Valid request: MigratePasswordRequest,
    ): HttpResponse<Map<String, String>> {
        val token = authService.migratePassword(request)
        return HttpResponse.ok(mapOf("token" to token))
    }
}
