package top.sunbath.api.memo.model

import io.micronaut.core.annotation.Creator
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Size
import top.sunbath.shared.dynamodb.Identified
import top.sunbath.shared.dynamodb.Indexable
import java.time.Instant

/**
 * A Memo entity.
 */
@Introspected
@Serdeable
class Memo :
    Identified,
    Indexable {
    companion object {
        fun getUserIdStatusPkValue(
            userId: String,
            isDeleted: Boolean,
            isCompleted: Boolean,
        ): String = "CLS#${Memo::class.simpleName}_USER_ID#${userId}_IS_DELETED#{$isDeleted}_IS_COMPLETED#$isCompleted"

        fun getTimeSkValue(
            createdAt: Instant,
            reminderTime: Instant?,
        ): String = "CREATED_AT#${createdAt}_REMINDER_TIME#${reminderTime?.toString() ?: "9999-12-31T23:59:59.999Z"}"
    }

    @get:NonNull
    override var id: String = ""

    @get:NonNull
    @get:Size(min = 0, max = 100)
    var title: String = ""

    @get:NonNull
    @get:Size(min = 0, max = 1000)
    var content: String = ""

    @get:NonNull
    var createdAt: Instant = Instant.now()

    @get:NonNull
    var updatedAt: Instant = Instant.now()

    @get:Nullable
    var reminderTime: Instant? = null

    @get:NonNull
    var isDeleted: Boolean = false

    @get:NonNull
    var isCompleted: Boolean = false

    @get:NonNull
    var userId: String = ""

    /**
     * Default constructor.
     */
    constructor()

    /**
     * Constructor with all properties.
     */
    @Creator
    constructor(
        id: String,
        title: String,
        content: String,
        reminderTime: Instant?,
        userId: String,
        isCompleted: Boolean,
        isDeleted: Boolean,
        createdAt: Instant,
        updatedAt: Instant,
    ) {
        this.id = id
        this.title = title
        this.content = content
        this.reminderTime = reminderTime
        this.userId = userId
        this.isCompleted = isCompleted
        this.isDeleted = isDeleted
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }

    /**
     * Returns index values for this user.
     */
    override fun getIndexValues(): Map<String, String> {
        val indexValues = mutableMapOf<String, String>()

        // Create index values for USER_FILTER_INDEX
        // Partition key: userId_isDeleted_isCompleted
        indexValues["USER_FILTER_PK"] = getUserIdStatusPkValue(userId, isDeleted, isCompleted)
        // Sort key: createdAt_reminderTime
        indexValues["USER_FILTER_SK"] = getTimeSkValue(createdAt, reminderTime)

        return indexValues
    }
}
