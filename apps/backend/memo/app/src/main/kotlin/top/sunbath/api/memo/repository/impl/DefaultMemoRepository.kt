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
import top.sunbath.api.memo.repository.MemoListFilter
import top.sunbath.api.memo.repository.MemoRepository
import top.sunbath.api.memo.repository.MemoSort
import top.sunbath.api.memo.repository.MemoSortOrder
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

        // Define index constants for USER_FILTER_INDEX
        private const val PK_USER_STATUS_SK_REMIDER_TIME_INDEX = "P_USER_STATUS_S_REMIDER_TIME_INDEX"
        private const val PK_USER_STATUS = "USER_FILTER_PK" // 分区键：USER_ID#userId_IS_DELETED#isDeleted_IS_COMPLETED#isCompleted
        private const val SK_REMIDER_TIME = "USER_FILTER_SK" // 排序键：REMINDER_TIME#reminderTime_CREATED_AT#createdAt

        // Register indexes
        init {
            DynamoRepository.registerIndex(IndexDefinition(PK_USER_STATUS_SK_REMIDER_TIME_INDEX, PK_USER_STATUS, SK_REMIDER_TIME))
        }
    }

    // Add a constructor init block to ensure indexes are registered
    init {
        // This ensures that the companion object's init block is executed
        // and the indexes are registered before the repository is used
        LOG.debug("Initializing DefaultMemoRepository with user_filter index: $PK_USER_STATUS_SK_REMIDER_TIME_INDEX")
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

        existingMemo.title = title
        existingMemo.content = content
        existingMemo.isCompleted = isCompleted
        existingMemo.isDeleted = isDeleted
        existingMemo.reminderTime = reminderTime

        save(existingMemo)
        return true
    }

    @NonNull
    override fun findAllWithCursor(
        limit: Int,
        lastEvaluatedId: String?,
        filter: MemoListFilter,
        sort: MemoSort,
    ): Pair<List<Memo>, String?> {
        if (limit <= 0) {
            return Pair(emptyList(), null)
        }

        val direction = sort.sortOrder == MemoSortOrder.ASC
        val skOperator = if (direction) ">" else "<"

        // Build partition key value
        val userFilterPkValue =
            Memo.getUserIdStatusPkValue(
                userId = filter.userId,
                isDeleted = filter.isDeleted,
                isCompleted = filter.isCompleted,
            )

        // Create query request
        val requestBuilder =
            QueryRequest
                .builder()
                .tableName(dynamoConfiguration.tableName)
                .indexName(PK_USER_STATUS_SK_REMIDER_TIME_INDEX)
                .scanIndexForward(direction) // Determine scan direction based on sort order
                .limit(limit)

        if (lastEvaluatedId == null) {
            // First query
            requestBuilder
                .keyConditionExpression("#pk = :pk")
                .expressionAttributeNames(
                    mapOf("#pk" to PK_USER_STATUS),
                ).expressionAttributeValues(
                    mapOf(":pk" to AttributeValue.builder().s(userFilterPkValue).build()),
                )
        } else {
            // Pagination query - need last evaluated ID from previous query
            // Get last item's sort key value
            val lastItem = findById(Memo::class.java, lastEvaluatedId)

            if (lastItem == null) {
                // If the item corresponding to the previous ID is not found, treat it as the first query
                return findAllWithCursor(limit, null, filter, sort)
            }

            // Build pagination query
            requestBuilder
                .keyConditionExpression("#pk = :pk AND #sk $skOperator :sk")
                .expressionAttributeNames(
                    mapOf(
                        "#pk" to PK_USER_STATUS,
                        "#sk" to SK_REMIDER_TIME,
                    ),
                ).expressionAttributeValues(
                    mapOf(
                        ":pk" to AttributeValue.builder().s(userFilterPkValue).build(),
                        ":sk" to AttributeValue.builder().s(lastItem[SK_REMIDER_TIME]!!.s()).build(),
                    ),
                )
        }

        val response = dynamoDbClient.query(requestBuilder.build())
        if (LOG.isTraceEnabled) {
            LOG.trace(response.toString())
        }
        val memos = parseInResponse(response)

        // Get next page cursor
        val nextCursor = lastEvaluatedId(response, Memo::class.java)

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
        entity.reminderTime?.let {
            result[ATTRIBUTE_REMINDER_TIME] = AttributeValue.builder().s(it.toString()).build()
        }
        result[ATTRIBUTE_IS_COMPLETED] = AttributeValue.builder().bool(entity.isCompleted).build()
        result[ATTRIBUTE_IS_DELETED] = AttributeValue.builder().bool(entity.isDeleted).build()

        return result
    }
}
