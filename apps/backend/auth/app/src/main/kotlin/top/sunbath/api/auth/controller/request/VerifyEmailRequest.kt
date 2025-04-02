package top.sunbath.api.auth.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank

/**
 * Request for email verification.
 */
@Introspected
@Serdeable
data class VerifyEmailRequest(
    /**
     * The verification token sent to the user's email.
     */
    @field:NotBlank(message = "Verification token is required")
    val token: String,
)
