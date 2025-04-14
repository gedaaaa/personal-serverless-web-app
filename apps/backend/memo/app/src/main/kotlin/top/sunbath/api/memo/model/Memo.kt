package top.sunbath.api.memo.model

import io.micronaut.core.annotation.Creator
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
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
    @get:NonNull
    override var id: String? = null

    @get:NonNull
    var title: String = ""

    @get:NonNull
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
    ) {
        this.id = id
        this.title = title
        this.content = content
        this.reminderTime = reminderTime
        this.userId = userId
        this.isCompleted = isCompleted
        this.isDeleted = isDeleted
    }

    /**
     * Returns index values for this user.
     */
    override fun getIndexValues(): Map<String, String> {
        val indexValues = mutableMapOf<String, String>()

        return indexValues
    }
}
