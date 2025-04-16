package top.sunbath.api.memo.controller.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import top.sunbath.api.memo.model.Memo
import java.time.Instant

@Introspected
@Serdeable
data class MemoResponse(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val reminderTime: Instant?,
    val isCompleted: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun fromMemo(memo: Memo): MemoResponse =
            MemoResponse(memo.id, memo.userId, memo.title, memo.content, memo.reminderTime, memo.isCompleted, memo.createdAt)
    }
}
