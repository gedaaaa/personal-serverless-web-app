package top.sunbath.api.auth.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Request object for creating a new user.
 */
@Introspected
@Serdeable
data class CreateUserRequest(
    @field:NotBlank
    val username: String,
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
        message = "Password must be at least 8 characters long and contain at least one letter and one number",
    )
    val password: String,
    val fullName: String?,
)
