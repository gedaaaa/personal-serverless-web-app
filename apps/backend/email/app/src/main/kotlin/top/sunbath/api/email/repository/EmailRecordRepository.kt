package top.sunbath.api.email.repository

import io.micronaut.core.annotation.NonNull
import jakarta.validation.constraints.NotBlank

/**
 * Repository interface for User entity operations.
 */
interface EmailRecordRepository {
    /**
     * Save a new user.
     * @param username The username
     * @param email The email
     * @param password The hashed password
     * @param roles The user roles
     * @param fullName The full name (optional)
     * @param emailVerified Whether the email is verified
     * @param emailVerificationToken The email verification token
     * @param emailVerificationTokenExpiresAt When the verification token expires
     * @param lastVerificationEmailSentAt When the last verification email was sent
     * @return The ID of the saved user
     */
    @NonNull
    fun save(
        @NonNull @NotBlank to: String,
        @NonNull @NotBlank from: String,
        @NonNull @NotBlank subject: String,
        @NonNull @NotBlank html: String,
        @NonNull vendorResponse: String,
    ): String
}
