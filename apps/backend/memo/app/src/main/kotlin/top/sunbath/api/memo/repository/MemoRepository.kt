package top.sunbath.api.memo.repository

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank
import top.sunbath.api.memo.model.Memo
import java.time.Instant

@Introspected
@Serdeable
data class MemoListFilter(
    val userId: String,
    val isCompleted: Boolean,
    val isDeleted: Boolean,
)

enum class MemoSortOrder {
    ASC,
    DESC,
}

enum class MemoSortKey(
    val value: String,
) {
    REMINDER_TIME("reminderTime"),
}

@Introspected
@Serdeable
data class MemoSort(
    @field:Nullable
    val sortOrder: MemoSortOrder? = MemoSortOrder.ASC,
    @field:Nullable
    val sortKey: MemoSortKey? = MemoSortKey.REMINDER_TIME,
)

/**
 * Repository interface for Memo entity operations.
 */
interface MemoRepository {
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
        filter: MemoListFilter,
        sort: MemoSort,
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
