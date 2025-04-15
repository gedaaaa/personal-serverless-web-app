package top.sunbath.api.memo.integration

import top.sunbath.api.memo.controller.request.CreateMemoRequest
import top.sunbath.api.memo.controller.request.UpdateMemoRequest
import top.sunbath.api.memo.model.Memo
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

object TestMemoFactory {
    fun createMemo(
        id: String = UUID.randomUUID().toString(),
        userId: String = "test-user-id",
        title: String = "Test Memo",
        content: String = "Test Content",
        reminderTime: Instant? = Instant.now().plus(1, ChronoUnit.DAYS),
        isCompleted: Boolean = false,
        isDeleted: Boolean = false,
    ): Memo {
        val memo =
            Memo(
                id = id,
                userId = userId,
                title = title,
                content = content,
                reminderTime = reminderTime,
                isCompleted = isCompleted,
                isDeleted = isDeleted,
            )

        // 这些属性不在构造函数中，需要单独设置
        val now = Instant.now()
        memo.createdAt = now
        memo.updatedAt = now

        return memo
    }

    fun createCreateMemoRequest(
        title: String = "Test Memo",
        content: String = "Test Content",
        reminderTime: Instant? = Instant.now().plus(1, ChronoUnit.DAYS),
    ): CreateMemoRequest =
        CreateMemoRequest(
            title = title,
            content = content,
            reminderTime = reminderTime,
        )

    fun createUpdateMemoRequest(
        title: String = "Updated Test Memo",
        content: String = "Updated Test Content",
        reminderTime: Instant? = Instant.now().plus(2, ChronoUnit.DAYS),
        isCompleted: Boolean = true,
    ): UpdateMemoRequest =
        UpdateMemoRequest(
            title = title,
            content = content,
            reminderTime = reminderTime,
            isCompleted = isCompleted,
        )
}
