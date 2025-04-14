package top.sunbath.api.memo.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.MemoRepository
import java.time.Instant

/**
 * Service for memo operations.
 */
@Singleton
class MemoService(
    private val memoRepository: MemoRepository,
) {
    private val logger = LoggerFactory.getLogger(Memo::class.java)

    fun createMemo(
        userId: String,
        title: String,
        content: String,
        reminderTime: Instant?,
    ): String {
        // TODO: notification
        return memoRepository.save(
            userId = userId,
            title = title,
            content = content,
            reminderTime = reminderTime,
        )
    }

    fun getMemoById(
        userId: String,
        id: String,
    ): Memo? {
        val memo = memoRepository.findById(id)
        if (memo == null) {
            return null
        }
        if (memo.userId != userId) {
            return null
        }
        return memo
    }

    fun getAllMemosWithCursor(
        userId: String,
        limit: Int,
        cursor: String?,
    ): Pair<List<Memo>, String?> {
        val memos =
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
            )

        // TODO: add userId filter
        logger.info("memos: $memos, userId: $userId")

        return memos
    }

    fun updateMemo(
        userId: String,
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

        if (existingMemo.userId != userId) {
            return false
        }

        // TODO: notification
        return memoRepository.update(
            id = id,
            title = title ?: existingMemo.title,
            content = content ?: existingMemo.content,
            reminderTime = reminderTime ?: existingMemo.reminderTime,
            isCompleted = isCompleted ?: existingMemo.isCompleted,
            isDeleted = isDeleted ?: existingMemo.isDeleted,
        )
    }

    fun deleteMemo(
        userId: String,
        id: String,
    ): Boolean {
        val memo = memoRepository.findById(id)
        if (memo == null) {
            return false
        }
        if (memo.userId != userId) {
            return false
        }
        // TODO: notification
        return memoRepository.update(
            id = id,
            title = memo.title,
            content = memo.content,
            reminderTime = memo.reminderTime,
            isCompleted = memo.isCompleted,
            isDeleted = true,
        )
    }
}
