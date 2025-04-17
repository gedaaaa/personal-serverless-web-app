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
 * A NotificationSchedule entity.
 */
@Introspected
@Serdeable
class NotificationSchedule :
    Identified,
    Indexable {
    @get:NonNull
    override var id: String = ""

    @get:NonNull
    var notificationId: String = ""

    @get:NonNull
    var createdAt: Instant = Instant.now()

    @get:NonNull
    var updatedAt: Instant = Instant.now()

    @get:Nullable
    var reminderTime: Instant? = null

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
        notificationId: String,
        createdAt: Instant,
        updatedAt: Instant,
        reminderTime: Instant?,
    ) {
        this.id = id
        this.notificationId = notificationId
        this.createdAt = createdAt
        this.updatedAt = updatedAt
        this.reminderTime = reminderTime
    }

    /**
     * Returns index values for this user.
     */
    override fun getIndexValues(): Map<String, String> {
        val indexValues = mutableMapOf<String, String>()
        return indexValues
    }
}
