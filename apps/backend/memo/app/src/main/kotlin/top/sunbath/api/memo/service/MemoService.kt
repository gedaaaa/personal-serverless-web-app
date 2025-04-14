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
        return memoRepository.save(userId, title, content, reminderTime)
    }

    fun getMemoById(id: String): Memo? = memoRepository.findById(id)

    fun getAllMemosWithCursor(
        limit: Int,
        cursor: String?,
    ): Pair<List<Memo>, String?> = memoRepository.findAllWithCursor(limit, cursor)

    fun updateMemo(memo: Memo): Boolean {
        val id = memo.id
        if (id == null) {
            return false
        }
        // TODO: notification
        return memoRepository.update(
            id = id,
            title = memo.title,
            content = memo.content,
            reminderTime = memo.reminderTime,
            isCompleted = memo.isCompleted,
            isDeleted = memo.isDeleted,
        )
    }

    fun deleteMemo(id: String): Boolean {
        val memo = memoRepository.findById(id)
        if (memo == null) {
            return false
        }
        memo.isDeleted = true
        return updateMemo(memo)
    }
}
