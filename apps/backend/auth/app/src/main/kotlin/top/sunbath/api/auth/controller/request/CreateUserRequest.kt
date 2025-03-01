package top.sunbath.api.auth.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

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
    val fullName: String?,
)
