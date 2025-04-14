package top.sunbath.api.memo.repository.impl

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.core.util.CollectionUtils
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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.MemoRepository
import top.sunbath.shared.dynamodb.DynamoConfiguration
import top.sunbath.shared.dynamodb.DynamoRepository
import top.sunbath.shared.dynamodb.IdGenerator
import top.sunbath.shared.dynamodb.IndexDefinition
import java.time.Instant

@Singleton
open class DefaultMemoRepository(
    dynamoDbClient: DynamoDbClient,
    dynamoConfiguration: DynamoConfiguration,
    private val idGenerator: IdGenerator,
) : DynamoRepository<Memo>(dynamoDbClient, dynamoConfiguration),
    MemoRepository {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DefaultMemoRepository::class.java)
        private const val ATTRIBUTE_ID = "id"
        private const val ATTRIBUTE_TITLE = "title"
        private const val ATTRIBUTE_CONTENT = "content"
        private const val ATTRIBUTE_REMINDER_TIME = "reminderTime"
        private const val ATTRIBUTE_IS_ARCHIVED = "isArchived"
        private const val ATTRIBUTE_USER_ID = "userId"
        private const val ATTRIBUTE_IS_COMPLETED = "isCompleted"
        private const val ATTRIBUTE_IS_DELETED = "isDeleted"

        // Define index constants
        private const val USER_ID_STATUS_INDEX = "USER_ID_STATUS_INDEX"
        private const val USER_ID_STATUS_PK = "USER_ID_STATUS_PK"
        private const val USER_ID_STATUS_SK = "USER_ID_STATUS_SK"

        // Register indexes
        init {
            DynamoRepository.registerIndex(IndexDefinition(USER_ID_STATUS_INDEX, USER_ID_STATUS_PK, USER_ID_STATUS_SK))
        }
    }

    // Add a constructor init block to ensure indexes are registered
    init {
        // This ensures that the companion object's init block is executed
        // and the indexes are registered before the repository is used
        LOG.debug("Initializing DefaultMemoRepository with user_id_status index: $USER_ID_STATUS_INDEX")
    }

    @NonNull
    override fun save(
        @NonNull @NotBlank userId: String,
        @NonNull @NotBlank title: String,
        @NonNull @NotBlank content: String,
        @NonNull reminderTime: Instant?,
    ): String {
        val id = idGenerator.generate()
        save(
            Memo(
                id = id,
                title = title,
                content = content,
                reminderTime = reminderTime,
                userId = userId,
                isCompleted = false,
                isDeleted = false,
            ),
        )
        return id
    }

    protected open fun save(
        @NonNull @NotNull @Valid memo: Memo,
    ) {
        val itemResponse: PutItemResponse =
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .item(item(memo))
                    .build(),
            )
        if (LOG.isDebugEnabled) {
            LOG.debug(itemResponse.toString())
        }
    }

    @NonNull
    override fun findById(
        @NonNull @NotBlank id: String,
    ): Memo? {
        return findById(Memo::class.java, id)?.let { return memoOf(it) }
    }

    override fun update(
        @NonNull @NotBlank id: String,
        @NonNull @NotBlank title: String,
        @NonNull @NotBlank content: String,
        @Nullable reminderTime: Instant?,
        @NonNull isCompleted: Boolean,
        @NonNull isDeleted: Boolean,
    ): Boolean {
        val existingMemo = findById(id) ?: return false

        // Update only the non-null fields
        title.let { existingMemo.title = it }
        content.let { existingMemo.content = it }
        isCompleted.let { existingMemo.isCompleted = it }
        isDeleted.let { existingMemo.isDeleted = it }
        reminderTime?.let { existingMemo.reminderTime = it }

        save(existingMemo)
        return true
    }

    @NonNull
    override fun findAll(): List<Memo> {
        val result = ArrayList<Memo>()
        var beforeId: String? = null
        do {
            val request: QueryRequest = findAllQueryRequest(Memo::class.java, beforeId, null)
            val response: QueryResponse = dynamoDbClient.query(request)
            if (LOG.isTraceEnabled) {
                LOG.trace(response.toString())
            }
            result.addAll(parseInResponse(response))
            beforeId = lastEvaluatedId(response, Memo::class.java).orElse(null)
        } while (beforeId != null)
        return result
    }

    @NonNull
    override fun findAllWithCursor(
        limit: Int,
        lastEvaluatedId: String?,
    ): Pair<List<Memo>, String?> {
        if (limit <= 0) {
            return Pair(emptyList(), null)
        }

        val request: QueryRequest = findAllQueryRequest(Memo::class.java, lastEvaluatedId, limit)
        val response: QueryResponse = dynamoDbClient.query(request)

        if (LOG.isTraceEnabled) {
            LOG.trace(response.toString())
        }

        val memos = parseInResponse(response)
        val nextCursor = lastEvaluatedId(response, Memo::class.java).orElse(null)

        return Pair(memos, nextCursor)
    }

    private fun parseInResponse(response: QueryResponse): List<Memo> {
        val items: List<Map<String, AttributeValue>> = response.items()
        val result = ArrayList<Memo>()
        if (CollectionUtils.isNotEmpty(items)) {
            for (item in items) {
                result.add(memoOf(item))
            }
        }
        return result
    }

    @NonNull
    private fun memoOf(
        @NonNull item: Map<String, AttributeValue>,
    ): Memo =
        Memo(
            id = item[ATTRIBUTE_ID]!!.s(),
            title = item[ATTRIBUTE_TITLE]!!.s(),
            content = item[ATTRIBUTE_CONTENT]!!.s(),
            userId = item[ATTRIBUTE_USER_ID]!!.s(),
            reminderTime = item[ATTRIBUTE_REMINDER_TIME]?.s()?.let { Instant.parse(it) },
            isCompleted = item[ATTRIBUTE_IS_COMPLETED]?.bool() ?: false,
            isDeleted = item[ATTRIBUTE_IS_DELETED]?.bool() ?: false,
        )

    @NonNull
    override fun item(
        @NonNull entity: Memo,
    ): Map<String, AttributeValue> {
        val result = super.item(entity).toMutableMap()
        result[ATTRIBUTE_ID] = AttributeValue.builder().s(entity.id).build()
        result[ATTRIBUTE_TITLE] = AttributeValue.builder().s(entity.title).build()
        result[ATTRIBUTE_CONTENT] = AttributeValue.builder().s(entity.content).build()
        result[ATTRIBUTE_USER_ID] = AttributeValue.builder().s(entity.userId).build()
        result[ATTRIBUTE_REMINDER_TIME] = AttributeValue.builder().s(entity.reminderTime?.toString()).build()
        result[ATTRIBUTE_IS_COMPLETED] = AttributeValue.builder().bool(entity.isCompleted).build()
        result[ATTRIBUTE_IS_DELETED] = AttributeValue.builder().bool(entity.isDeleted).build()

        return result
    }
}
