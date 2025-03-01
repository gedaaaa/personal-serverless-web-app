package top.sunbath.api.auth.repository

import io.micronaut.core.annotation.NonNull
import jakarta.validation.constraints.NotBlank
import top.sunbath.api.auth.model.User

/**
 * Repository interface for User entity operations.
 */
interface UserRepository {
    /**
     * Find all users.
     * @return List of all users
     */
    @NonNull
    fun findAll(): List<User>

    /**
     * Find a user by ID.
     * @param id The user ID
     * @return The user if found
     */
    @NonNull
    fun findById(
        @NonNull @NotBlank id: String,
    ): User?

    /**
     * Delete a user by ID.
     * @param id The user ID
     */
    fun delete(
        @NonNull @NotBlank id: String,
    )

    /**
     * Save a new user.
     * @param username The username
     * @param email The email
     * @param fullName The full name (optional)
     * @return The ID of the saved user
     */
    @NonNull
    fun save(
        @NonNull @NotBlank username: String,
        @NonNull @NotBlank email: String,
        fullName: String?,
    ): String
}
