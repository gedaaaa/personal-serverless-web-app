package top.sunbath.api.memo.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.memo.controller.request.GetMemoListRequestFilter
import top.sunbath.api.memo.controller.request.GetMemoListRequestSort
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.MemoListFilter
import top.sunbath.api.memo.repository.MemoRepository
import top.sunbath.api.memo.repository.MemoSort
import top.sunbath.api.memo.repository.MemoSortKey
import top.sunbath.api.memo.repository.MemoSortOrder
import top.sunbath.shared.types.CurrentUser
import java.time.Instant

/**
 * Service for memo operations.
 */
@Singleton
class MemoService(
    private val memoRepository: MemoRepository,
    private val notificationScheduleService: NotificationScheduleService,
) {
    private val logger = LoggerFactory.getLogger(MemoService::class.java)

    /**
     * Create a new memo.
     * @param userInfo The current user.
     * @param title The title of the memo.
     * @param content The content of the memo.
     * @param reminderTime The reminder time of the memo.
     * @return The id of the memo.
     */
    fun createMemo(
        userInfo: CurrentUser,
        title: String,
        content: String,
        reminderTime: Instant?,
    ): String {
        val memoId =
            memoRepository.save(
                userId = userInfo.id,
                title = title,
                content = content,
                reminderTime = reminderTime,
            )

        handleNotificationSchedule(memoId, userInfo)

        return memoId
    }

    /**
     * Get a memo by id.
     * @param userInfo The current user.
     * @param id The id of the memo.
     * @return The memo.
     */
    fun getMemoById(
        userInfo: CurrentUser,
        id: String,
    ): Memo? {
        val memo = memoRepository.findById(id) ?: return null
        if (memo.userId != userInfo.id) {
            logger.warn("User ${userInfo.id} attempted to access memo $id owned by ${memo.userId}")
            return null
        }
        return memo
    }

    /**
     * Get all memos with pagination.
     * @param userInfo The current user.
     * @param limit The limit of the memos.
     * @param cursor The cursor of the memos.
     * @param filter The filter of the memos.
     * @param sort The sort of the memos.
     * @return The memos.
     */
    fun getAllMemosWithCursor(
        userInfo: CurrentUser,
        limit: Int,
        cursor: String?,
        filter: GetMemoListRequestFilter?,
        sort: GetMemoListRequestSort?,
    ): Pair<List<Memo>, String?> {
        logger.info("getAllMemosWithCursor: filter: $filter, sort: $sort")
        val repoFilter =
            if (filter == null) {
                MemoListFilter(
                    userId = userInfo.id,
                    isCompleted = false,
                    isDeleted = false,
                )
            } else {
                MemoListFilter(
                    userId = userInfo.id,
                    isCompleted = filter.isCompleted ?: false,
                    isDeleted = filter.isDeleted ?: false,
                )
            }

        val repoSort =
            if (sort == null) {
                MemoSort(
                    sortOrder = MemoSortOrder.ASC,
                    sortKey = MemoSortKey.REMINDER_TIME,
                )
            } else {
                MemoSort(
                    sortOrder = sort.sortOrder,
                    sortKey = sort.sortKey,
                )
            }

        val memos =
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter = repoFilter,
                sort = repoSort,
            )

        return memos
    }

    /**
     * Update a memo.
     * @param userInfo The current user.
     * @param id The id of the memo.
     * @param title The title of the memo.
     * @param content The content of the memo.
     * @param reminderTime The reminder time of the memo.
     * @param isCompleted The completion status of the memo.
     * @param isDeleted The deletion status of the memo.
     * @return The success of the update.
     */
    fun updateMemo(
        userInfo: CurrentUser,
        id: String,
        title: String,
        content: String,
        reminderTime: Instant?,
        isCompleted: Boolean,
        isDeleted: Boolean?,
    ): Boolean {
        val originalMemo =
            memoRepository.findById(id)
                ?: return false

        if (originalMemo.userId != userInfo.id) {
            logger.warn("User ${userInfo.id} attempted to update memo $id owned by ${originalMemo.userId}")
            return false
        }

        val updateSuccess =
            memoRepository.update(
                id = id,
                title = title,
                content = content,
                reminderTime = reminderTime,
                isCompleted = isCompleted,
                isDeleted = isDeleted ?: originalMemo.isDeleted,
            )

        if (updateSuccess) {
            handleNotificationSchedule(id, userInfo)
        }

        return updateSuccess
    }

    /**
     * Delete a memo.
     * @param userInfo The current user.
     * @param id The id of the memo.
     * @return The success of the deletion.
     */
    fun deleteMemo(
        userInfo: CurrentUser,
        id: String,
    ): Boolean {
        val memoToDelete =
            memoRepository.findById(id)
                ?: return true

        if (memoToDelete.userId != userInfo.id) {
            logger.warn("User ${userInfo.id} attempted to delete memo $id owned by ${memoToDelete.userId}. Skipping deletion.")
            return false
        }

        if (memoToDelete.isDeleted) {
            logger.info("Memo $id is already marked as deleted.")
            return true
        }

        val updateSuccess =
            memoRepository.update(
                id = id,
                title = memoToDelete.title,
                content = memoToDelete.content,
                reminderTime = memoToDelete.reminderTime,
                isCompleted = memoToDelete.isCompleted,
                isDeleted = true,
            )

        if (updateSuccess) {
            handleNotificationSchedule(id, userInfo)
        } else {
            logger.error("Failed to mark memo $id as deleted in repository.")
        }

        return updateSuccess
    }

    /**
     * Handle the notification schedule.
     * @param memoId The id of the memo.
     * @param userInfo The current user.
     */
    private fun handleNotificationSchedule(
        memoId: String,
        userInfo: CurrentUser,
    ) {
        try {
            val memo = memoRepository.findById(memoId)
            if (memo != null) {
                notificationScheduleService.handleNotificationSchedule(memo, userInfo)
            }
        } catch (e: Exception) {
            logger.error("Error handling notification schedule", e)
        }
    }
}
