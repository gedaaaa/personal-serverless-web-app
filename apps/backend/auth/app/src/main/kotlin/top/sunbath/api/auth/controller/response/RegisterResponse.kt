package top.sunbath.api.auth.controller.response

import io.micronaut.serde.annotation.Serdeable

/**
 * Response for user registration.
 *
 * @property userId The ID of the created user
 * @property message A message describing the registration status
 */
@Serdeable
data class RegisterResponse(
    val userId: String,
    val message: String,
)
