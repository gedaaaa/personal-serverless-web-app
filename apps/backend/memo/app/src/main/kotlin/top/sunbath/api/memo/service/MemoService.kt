package top.sunbath.api.memo.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.MemoListFilter
import top.sunbath.api.memo.repository.MemoRepository
import top.sunbath.api.memo.repository.MemoSort
import top.sunbath.api.memo.repository.MemoSortKey
import top.sunbath.api.memo.repository.MemoSortOrder
import top.sunbath.shared.types.UserInfo
import java.time.Instant

/**
 * Service for memo operations.
 */
@Singleton
class MemoService(
    private val memoRepository: MemoRepository,
    private val notificationScheduleService: NotificationScheduleService,
) {
    private val logger = LoggerFactory.getLogger(Memo::class.java)

    fun createMemo(
        userInfo: UserInfo,
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

        handleNotificationSchedule(memoId, userInfo.email)

        return memoId
    }

    fun getMemoById(
        userInfo: UserInfo,
        id: String,
    ): Memo? {
        val memo = memoRepository.findById(id)
        if (memo == null) {
            return null
        }
        if (memo.userId != userInfo.id) {
            return null
        }
        return memo
    }

    fun getAllMemosWithCursor(
        userInfo: UserInfo,
        limit: Int,
        cursor: String?,
    ): Pair<List<Memo>, String?> {
        val memos =
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter =
                    MemoListFilter(
                        userId = userInfo.id,
                        isCompleted = false,
                        isDeleted = false,
                    ),
                sort =
                    MemoSort(
                        sortOrder = MemoSortOrder.ASC,
                        sortKey = MemoSortKey.REMINDER_TIME,
                    ),
            )

        return memos
    }

    fun updateMemo(
        userInfo: UserInfo,
        id: String,
        title: String?,
        content: String?,
        reminderTime: Instant?,
        isCompleted: Boolean?,
        isDeleted: Boolean?,
    ): Boolean {
        val existingMemo = memoRepository.findById(id)
        if (existingMemo == null) {
            return false
        }

        if (existingMemo.userId != userInfo.id) {
            return false
        }

        val updateSuccess =
            memoRepository.update(
                id = id,
                title = title ?: existingMemo.title,
                content = content ?: existingMemo.content,
                reminderTime = reminderTime ?: existingMemo.reminderTime,
                isCompleted = isCompleted ?: existingMemo.isCompleted,
                isDeleted = isDeleted ?: existingMemo.isDeleted,
            )

        if (updateSuccess) {
            handleNotificationSchedule(id, userInfo.email)
        }

        return updateSuccess
    }

    fun deleteMemo(
        userInfo: UserInfo,
        id: String,
    ): Boolean {
        val memo = memoRepository.findById(id)
        if (memo == null) {
            return false
        }
        if (memo.userId != userInfo.id) {
            return false
        }
        val updateSuccess =
            memoRepository.update(
                id = id,
                title = memo.title,
                content = memo.content,
                reminderTime = memo.reminderTime,
                isCompleted = memo.isCompleted,
                isDeleted = true,
            )

        if (updateSuccess) {
            handleNotificationSchedule(id, userInfo.email)
        }

        return updateSuccess
    }

    private fun handleNotificationSchedule(
        memoId: String,
        userEmail: String,
    ) {
        try {
            val memo = memoRepository.findById(memoId)
            if (memo != null) {
                notificationScheduleService.handleNotificationSchedule(memo, userEmail)
            }
        } catch (e: Exception) {
            logger.error("Error handling notification schedule", e)
        }
    }
}
