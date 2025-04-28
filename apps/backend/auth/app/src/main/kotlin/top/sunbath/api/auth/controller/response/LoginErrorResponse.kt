package top.sunbath.api.auth.controller.response

import io.micronaut.serde.annotation.Serdeable
import java.time.Instant

/**
 * Response for login errors, especially when password migration is needed.
 */
@Serdeable
data class LoginErrorResponse(
    /**
     * The error message.
     */
    val message: String,
    /**
     * The error type.
     */
    val errorType: String,
    /**
     * The migration token for password migration (if applicable).
     */
    val migrationToken: String?,
    /**
     * The expiration time of the migration token (if applicable).
     */
    val migrationTokenExpiresAt: Instant?,
)
