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
     * Find users with cursor-based pagination.
     * @param limit The maximum number of items to return
     * @param lastEvaluatedId The ID of the last evaluated item from the previous page (null for first page)
     * @return Pair of users list and the last evaluated ID (null if no more pages)
     */
    @NonNull
    fun findAllWithCursor(
        limit: Int,
        lastEvaluatedId: String?,
    ): Pair<List<User>, String?>

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
     * Find a user by username.
     * @param username The username
     * @return The user if found
     */
    @NonNull
    fun findByUsername(
        @NonNull @NotBlank username: String,
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
     * @param password The hashed password
     * @param roles The user roles
     * @param fullName The full name (optional)
     * @return The ID of the saved user
     */
    @NonNull
    fun save(
        @NonNull @NotBlank username: String,
        @NonNull @NotBlank email: String,
        @NonNull @NotBlank password: String,
        @NonNull roles: Set<String>,
        fullName: String?,
    ): String

    /**
     * Update an existing user.
     * @param id The user ID
     * @param email The updated email (optional)
     * @param password The updated password (optional)
     * @param roles The updated roles (optional)
     * @param fullName The updated full name (optional)
     * @return True if the user was updated, false otherwise
     */
    fun update(
        @NonNull @NotBlank id: String,
        email: String?,
        password: String?,
        roles: Set<String>?,
        fullName: String?,
    ): Boolean
}
