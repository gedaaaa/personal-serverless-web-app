package top.sunbath.api.auth.controller.response

import io.micronaut.serde.annotation.Serdeable

/**
 * Response for email verification.
 */
@Serdeable
data class VerifyEmailResponse(
    /**
     * The message describing the result of the verification.
     */
    val message: String,
)
