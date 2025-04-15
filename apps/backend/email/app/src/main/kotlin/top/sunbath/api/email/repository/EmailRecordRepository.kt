package top.sunbath.api.email.repository

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import jakarta.validation.constraints.NotBlank
import top.sunbath.api.email.model.EmailRecord

/**
 * Repository interface for Email Record operations.
 */
interface EmailRecordRepository {
    /**
     * Save a new email record.
     * @param to The recipient's email address
     * @param from The sender's email address
     * @param subject The email subject
     * @param html The email HTML content
     * @param vendorResponse The vendor's response
     * @return The ID of the saved record
     */
    @NonNull
    fun save(
        @NonNull @NotBlank to: String,
        @NonNull @NotBlank from: String,
        @NonNull @NotBlank subject: String,
        @NonNull @NotBlank html: String,
        @NonNull vendorResponse: String,
    ): String

    /**
     * Find an email record by ID.
     * @param id The record ID
     * @return The email record if found, null otherwise
     */
    @Nullable
    fun findById(id: String): EmailRecord?
}
