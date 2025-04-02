package top.sunbath.api.auth.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.LoginRequest
import top.sunbath.api.auth.controller.response.RegisterResponse
import top.sunbath.api.auth.service.AuthService

/**
 * Controller for authentication operations.
 */
@Validated
@Controller("/")
@Secured(SecurityRule.IS_ANONYMOUS)
class AuthController(
    private val authService: AuthService,
) {
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
    ): HttpResponse<Map<String, String>> {
        val token = authService.login(request)
        return HttpResponse.ok(mapOf("token" to token))
    }
}
