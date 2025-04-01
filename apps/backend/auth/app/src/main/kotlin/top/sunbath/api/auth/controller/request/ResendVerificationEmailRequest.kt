package top.sunbath.api.auth.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/**
 * Request for resending verification email.
 */
@Introspected
@Serdeable
data class ResendVerificationEmailRequest(
    /**
     * The email address to resend verification email to.
     */
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,
)
