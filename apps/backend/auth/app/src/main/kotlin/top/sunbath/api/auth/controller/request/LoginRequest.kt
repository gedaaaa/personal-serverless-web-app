package top.sunbath.api.auth.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank

/**
 * Request object for user login.
 */
@Introspected
@Serdeable
data class LoginRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    val password: String,
)
