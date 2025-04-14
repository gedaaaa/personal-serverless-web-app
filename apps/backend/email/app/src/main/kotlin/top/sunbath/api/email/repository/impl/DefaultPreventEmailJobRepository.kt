package top.sunbath.api.email.repository.impl

import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import jakarta.validation.constraints.NotBlank
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse
import top.sunbath.api.email.model.PreventEmailJob
import top.sunbath.api.email.repository.PreventEmailJobRepository
import top.sunbath.shared.dynamodb.DynamoConfiguration
import top.sunbath.shared.dynamodb.DynamoRepository

@Singleton
open class DefaultPreventEmailJobRepository(
    dynamoDbClient: DynamoDbClient,
    dynamoConfiguration: DynamoConfiguration,
) : DynamoRepository<PreventEmailJob>(dynamoDbClient, dynamoConfiguration),
    PreventEmailJobRepository {
    private val logger = LoggerFactory.getLogger(DefaultPreventEmailJobRepository::class.java)

    companion object {
        private const val ATTRIBUTE_ID = "id"
    }

    // Add a constructor init block to ensure indexes are registered
    init {
        // This ensures that the companion object's init block is executed
        // and the indexes are registered before the repository is used
        logger.debug("Initializing DefaultEmailRecordRepository")
    }

    override fun findById(id: String): PreventEmailJob? {
        return findById(PreventEmailJob::class.java, id)?.let { return preventEmailJobOf(it) }
    }

    @NonNull
    override fun save(
        @NonNull @NotBlank id: String,
    ): String {
        save(
            PreventEmailJob(
                id = id,
            ),
        )
        return id
    }

    private fun save(preventEmailJob: PreventEmailJob) {
        val itemResponse: PutItemResponse =
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .item(item(preventEmailJob))
                    .build(),
            )
        if (logger.isDebugEnabled) {
            logger.debug(itemResponse.toString())
        }
    }

    @NonNull
    override fun item(
        @NonNull entity: PreventEmailJob,
    ): Map<String, AttributeValue> {
        val result = super.item(entity).toMutableMap()
        result[ATTRIBUTE_ID] = AttributeValue.builder().s(entity.id).build()
        return result
    }

    private fun preventEmailJobOf(item: Map<String, AttributeValue>): PreventEmailJob =
        PreventEmailJob(
            id = item[ATTRIBUTE_ID]?.s() ?: throw IllegalArgumentException("id is required"),
        )
}
