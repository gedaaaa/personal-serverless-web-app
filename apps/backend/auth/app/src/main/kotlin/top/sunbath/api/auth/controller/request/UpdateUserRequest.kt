package top.sunbath.api.auth.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Pattern

/**
 * Request object for updating an existing user.
 */
@Introspected
@Serdeable
data class UpdateUserRequest(
    @field:Email
    val email: String?,
    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
        message = "Password must be at least 8 characters long and contain at least one letter and one number",
    )
    val password: String?,
    val fullName: String?,
    val roles: Set<String>?,
)
