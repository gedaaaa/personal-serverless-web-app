package top.sunbath.api.auth.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank

/**
 * Request for password migration from plain text to SHA-256.
 */
@Introspected
@Serdeable
data class MigratePasswordRequest(
    /**
     * The username of the user.
     */
    @field:NotBlank(message = "Username is required")
    val username: String,
    /**
     * The original plain text password for verification.
     */
    @field:NotBlank(message = "Original password is required")
    val originalPassword: String,
    /**
     * The desired SHA-256 hashed password.
     */
    @field:NotBlank(message = "Desired password is required")
    val desiredPassword: String,
    /**
     * The migration token issued during login.
     */
    @field:NotBlank(message = "Migration token is required")
    val migrationToken: String,
)
