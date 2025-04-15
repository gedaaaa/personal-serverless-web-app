package top.sunbath.api.memo.repository.impl

import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import top.sunbath.api.memo.model.NotificationSchedule
import top.sunbath.api.memo.repository.NotificationScheduleRepository
import top.sunbath.shared.dynamodb.DynamoConfiguration
import top.sunbath.shared.dynamodb.DynamoRepository
import java.time.Instant

@Singleton
open class DefaultNotificationScheduleRepository(
    dynamoDbClient: DynamoDbClient,
    dynamoConfiguration: DynamoConfiguration,
) : DynamoRepository<NotificationSchedule>(dynamoDbClient, dynamoConfiguration),
    NotificationScheduleRepository {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DefaultMemoRepository::class.java)
        private const val ATTRIBUTE_ID = "id"
        private const val ATTRIBUTE_NOTIFICATION_ID = "notificationId"
        private const val ATTRIBUTE_REMINDER_TIME = "reminderTime"
        private const val ATTRIBUTE_CREATED_AT = "createdAt"
        private const val ATTRIBUTE_UPDATED_AT = "updatedAt"

        // Register indexes
        init {
            // No indexes
        }
    }

    // Add a constructor init block to ensure indexes are registered
    init {
        // This ensures that the companion object's init block is executed
        // and the indexes are registered before the repository is used
        LOG.debug("Initializing DefaultMemoRepository")
    }

    /**
     * Save a notification schedule.
     * @param id The ID of the notification schedule
     * @param notificationId The ID of the notification
     * @param reminderTime The reminder time
     * @return The ID of the saved notification schedule
     */
    @NonNull
    override fun save(
        @NonNull @NotBlank id: String,
        @NonNull @NotBlank notificationId: String,
        @NonNull reminderTime: Instant,
    ): String {
        save(
            NotificationSchedule(
                id = id,
                notificationId = notificationId,
                reminderTime = reminderTime,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            ),
        )
        return id
    }

    /**
     * Save a notification schedule.
     * @param notificationSchedule The notification schedule to save
     */
    protected open fun save(
        @NonNull @NotNull @Valid notificationSchedule: NotificationSchedule,
    ) {
        val itemResponse: PutItemResponse =
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .item(item(notificationSchedule))
                    .build(),
            )
        if (LOG.isDebugEnabled) {
            LOG.debug(itemResponse.toString())
        }
    }

    /**
     * Find a notification schedule by ID.
     * @param id The ID of the notification schedule
     * @return The notification schedule if found
     */
    @NonNull
    override fun findById(
        @NonNull @NotBlank id: String,
    ): NotificationSchedule? {
        return findById(NotificationSchedule::class.java, id)?.let { return notificationScheduleOf(it) }
    }

    /**
     * Delete a notification schedule by ID.
     * @param id The ID of the notification schedule
     */
    override fun delete(
        @NonNull @NotBlank id: String,
    ) {
        delete(NotificationSchedule::class.java, id)
    }

    /**
     * Convert a map of attribute values to a NotificationSchedule entity.
     * @param item The map of attribute values
     * @return The NotificationSchedule entity
     */
    @NonNull
    private fun notificationScheduleOf(
        @NonNull item: Map<String, AttributeValue>,
    ): NotificationSchedule =
        NotificationSchedule(
            id = item[ATTRIBUTE_ID]!!.s(),
            notificationId = item[ATTRIBUTE_NOTIFICATION_ID]!!.s(),
            reminderTime = item[ATTRIBUTE_REMINDER_TIME]!!.s().let { Instant.parse(it) },
            createdAt = item[ATTRIBUTE_CREATED_AT]!!.s().let { Instant.parse(it) },
            updatedAt = item[ATTRIBUTE_UPDATED_AT]!!.s().let { Instant.parse(it) },
        )

    /**
     * Convert a NotificationSchedule entity to a map of attribute values.
     * @param entity The NotificationSchedule entity
     * @return A map of attribute values
     */
    @NonNull
    override fun item(
        @NonNull entity: NotificationSchedule,
    ): Map<String, AttributeValue> {
        val result = super.item(entity).toMutableMap()
        result[ATTRIBUTE_ID] = AttributeValue.builder().s(entity.id).build()
        result[ATTRIBUTE_NOTIFICATION_ID] = AttributeValue.builder().s(entity.notificationId).build()
        result[ATTRIBUTE_REMINDER_TIME] = AttributeValue.builder().s(entity.reminderTime?.toString()).build()
        result[ATTRIBUTE_CREATED_AT] = AttributeValue.builder().s(entity.createdAt.toString()).build()
        result[ATTRIBUTE_UPDATED_AT] = AttributeValue.builder().s(entity.updatedAt.toString()).build()

        return result
    }
}
