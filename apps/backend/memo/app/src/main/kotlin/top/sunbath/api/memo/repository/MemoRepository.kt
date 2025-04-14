package top.sunbath.api.memo.repository

import io.micronaut.core.annotation.NonNull
import jakarta.validation.constraints.NotBlank
import top.sunbath.api.memo.model.Memo
import java.time.Instant

/**
 * Repository interface for Memo entity operations.
 */
interface MemoRepository {
    /**
     * Find all memos.
     * @return List of all memos
     */
    @NonNull
    fun findAll(): List<Memo>

    /**
     * Find memos with cursor-based pagination.
     * @param limit The maximum number of items to return
     * @param lastEvaluatedId The ID of the last evaluated item from the previous page (null for first page)
     * @return Pair of memos list and the last evaluated ID (null if no more pages)
     */
    @NonNull
    fun findAllWithCursor(
        limit: Int,
        lastEvaluatedId: String?,
    ): Pair<List<Memo>, String?>

    /**
     * Find a memo by ID.
     * @param id The memo ID
     * @return The memo if found
     */
    @NonNull
    fun findById(
        @NonNull @NotBlank id: String,
    ): Memo?

    /**
     * Save a new memo.
     */
    @NonNull
    fun save(
        @NonNull @NotBlank userId: String,
        @NonNull @NotBlank title: String,
        @NonNull @NotBlank content: String,
        @NonNull reminderTime: Instant?,
    ): String

    /**
     * Update an existing memo.
     */
    fun update(
        @NonNull @NotBlank id: String,
        @NonNull @NotBlank title: String,
        @NonNull @NotBlank content: String,
        @NonNull reminderTime: Instant?,
        @NonNull isCompleted: Boolean,
        @NonNull isDeleted: Boolean,
    ): Boolean
}
