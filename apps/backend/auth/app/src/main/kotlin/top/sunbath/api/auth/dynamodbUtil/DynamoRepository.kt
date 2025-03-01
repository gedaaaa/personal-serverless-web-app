package top.sunbath.api.auth.dynamodbUtil

import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import top.sunbath.api.auth.config.DynamoConfiguration
import java.util.Arrays
import java.util.Collections
import java.util.Optional

@Requires(condition = CIAwsRegionProviderChainCondition::class)
@Requires(condition = CIAwsCredentialsProviderChainCondition::class)
@Requires(beans = [DynamoConfiguration::class, DynamoDbClient::class])
@Singleton
@Primary
open class DynamoRepository<T : Identified>(
    protected val dynamoDbClient: DynamoDbClient,
    protected val dynamoConfiguration: DynamoConfiguration,
) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DynamoRepository::class.java)
        protected const val HASH = "#"
        protected const val ATTRIBUTE_PK = "pk"
        protected const val ATTRIBUTE_SK = "sk"
        protected const val ATTRIBUTE_GSI_1_PK = "GSI1PK"
        protected const val ATTRIBUTE_GSI_1_SK = "GSI1SK"
        protected const val INDEX_GSI_1 = "GSI1"

        private val SUPPORTED_INDEXES =
            mutableListOf(
                IndexDefinition(INDEX_GSI_1, ATTRIBUTE_GSI_1_PK, ATTRIBUTE_GSI_1_SK),
            )

        fun registerIndex(indexDefinition: IndexDefinition) {
            if (SUPPORTED_INDEXES.none { it.indexName == indexDefinition.indexName }) {
                SUPPORTED_INDEXES.add(indexDefinition)
            }
        }

        fun getIndexDefinitions(): List<IndexDefinition> = SUPPORTED_INDEXES.toList()

        @NonNull
        fun lastEvaluatedId(
            @NonNull response: QueryResponse,
            @NonNull cls: Class<*>,
        ): Optional<String> {
            if (response.hasLastEvaluatedKey()) {
                val item = response.lastEvaluatedKey()
                if (item != null && item.containsKey(ATTRIBUTE_PK)) {
                    return id(cls, item[ATTRIBUTE_PK]!!)
                }
            }
            return Optional.empty()
        }

        private fun gsi1(): GlobalSecondaryIndex =
            GlobalSecondaryIndex
                .builder()
                .indexName(INDEX_GSI_1)
                .keySchema(
                    KeySchemaElement
                        .builder()
                        .attributeName(ATTRIBUTE_GSI_1_PK)
                        .keyType(KeyType.HASH)
                        .build(),
                    KeySchemaElement
                        .builder()
                        .attributeName(ATTRIBUTE_GSI_1_SK)
                        .keyType(KeyType.RANGE)
                        .build(),
                ).projection(
                    Projection
                        .builder()
                        .projectionType(ProjectionType.ALL)
                        .build(),
                ).build()

        private fun buildGlobalSecondaryIndex(indexDefinition: IndexDefinition): GlobalSecondaryIndex =
            GlobalSecondaryIndex
                .builder()
                .indexName(indexDefinition.indexName)
                .keySchema(
                    KeySchemaElement
                        .builder()
                        .attributeName(indexDefinition.partitionKeyName)
                        .keyType(KeyType.HASH)
                        .build(),
                    KeySchemaElement
                        .builder()
                        .attributeName(indexDefinition.sortKeyName)
                        .keyType(KeyType.RANGE)
                        .build(),
                ).projection(
                    Projection
                        .builder()
                        .projectionType(ProjectionType.ALL)
                        .build(),
                ).build()

        @NonNull
        protected fun classAttributeValue(
            @NonNull cls: Class<*>,
        ): AttributeValue =
            AttributeValue
                .builder()
                .s(cls.simpleName)
                .build()

        @NonNull
        protected fun id(
            @NonNull cls: Class<*>,
            @NonNull id: String,
        ): AttributeValue =
            AttributeValue
                .builder()
                .s("${cls.simpleName.uppercase()}$HASH$id")
                .build()

        @NonNull
        protected fun id(
            @NonNull cls: Class<*>,
            @NonNull attributeValue: AttributeValue,
        ): Optional<String> {
            val str = attributeValue.s()
            val substring = cls.simpleName.uppercase() + HASH
            return if (str.startsWith(substring)) Optional.of(str.substring(substring.length)) else Optional.empty()
        }
    }

    fun existsTable(): Boolean =
        try {
            dynamoDbClient.describeTable(
                DescribeTableRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .build(),
            )
            true
        } catch (e: ResourceNotFoundException) {
            false
        }

    /**
     * Updates the table's indexes if new ones have been registered.
     */
    fun updateTableIndexes() {
        try {
            // Get current table description
            val tableDescription =
                dynamoDbClient
                    .describeTable(
                        DescribeTableRequest
                            .builder()
                            .tableName(dynamoConfiguration.tableName)
                            .build(),
                    ).table()

            // Get existing GSIs
            val existingGSIs = tableDescription.globalSecondaryIndexes() ?: emptyList()
            val existingGSINames = existingGSIs.map { it.indexName() }.toSet()

            // Find indexes that need to be added
            val indexesToAdd =
                SUPPORTED_INDEXES.filter {
                    !existingGSINames.contains(it.indexName)
                }

            if (indexesToAdd.isNotEmpty()) {
                LOG.info("Adding ${indexesToAdd.size} new indexes to table ${dynamoConfiguration.tableName}")

                // Create attribute definitions for new indexes
                val attributeDefinitions = mutableListOf<AttributeDefinition>()
                indexesToAdd.forEach { index ->
                    attributeDefinitions.add(
                        AttributeDefinition
                            .builder()
                            .attributeName(index.partitionKeyName)
                            .attributeType(ScalarAttributeType.S)
                            .build(),
                    )
                    attributeDefinitions.add(
                        AttributeDefinition
                            .builder()
                            .attributeName(index.sortKeyName)
                            .attributeType(ScalarAttributeType.S)
                            .build(),
                    )
                }

                // Create GSIs for new indexes
                val gsisToAdd =
                    indexesToAdd.map {
                        if (it.indexName == INDEX_GSI_1) gsi1() else buildGlobalSecondaryIndex(it)
                    }

                // Update the table with new indexes
                dynamoDbClient.updateTable(
                    software.amazon.awssdk.services.dynamodb.model.UpdateTableRequest
                        .builder()
                        .tableName(dynamoConfiguration.tableName)
                        .attributeDefinitions(attributeDefinitions)
                        .globalSecondaryIndexUpdates(
                            gsisToAdd.map { gsi ->
                                software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndexUpdate
                                    .builder()
                                    .create(
                                        software.amazon.awssdk.services.dynamodb.model.CreateGlobalSecondaryIndexAction
                                            .builder()
                                            .indexName(gsi.indexName())
                                            .keySchema(gsi.keySchema())
                                            .projection(gsi.projection())
                                            .build(),
                                    ).build()
                            },
                        ).build(),
                )

                LOG.info("Started adding new indexes to table ${dynamoConfiguration.tableName}")

                // Wait for indexes to become active (optional, can be removed if not needed)
                waitForIndexesToBecomeActive(indexesToAdd.map { it.indexName })
            } else {
                LOG.info("No new indexes to add to table ${dynamoConfiguration.tableName}")
            }
        } catch (e: Exception) {
            LOG.error("Error updating table indexes", e)
            throw e
        }
    }

    /**
     * Waits for the specified indexes to become active.
     * This is an optional step but helps ensure indexes are ready before use.
     */
    private fun waitForIndexesToBecomeActive(indexNames: List<String>) {
        if (indexNames.isEmpty()) return

        LOG.info("Waiting for indexes ${indexNames.joinToString(", ")} to become active")

        var allActive = false
        var attempts = 0
        val maxAttempts = 60 // Maximum wait time: 10 minutes (60 * 10 seconds)

        while (!allActive && attempts < maxAttempts) {
            try {
                Thread.sleep(10000) // Wait 10 seconds between checks

                val tableDescription =
                    dynamoDbClient
                        .describeTable(
                            DescribeTableRequest
                                .builder()
                                .tableName(dynamoConfiguration.tableName)
                                .build(),
                        ).table()

                val gsiStatuses =
                    tableDescription
                        .globalSecondaryIndexes()
                        ?.filter { indexNames.contains(it.indexName()) }
                        ?.map { it.indexName() to it.indexStatus() }
                        ?.toMap() ?: emptyMap()

                if (gsiStatuses.size == indexNames.size &&
                    gsiStatuses.values.all { it == software.amazon.awssdk.services.dynamodb.model.IndexStatus.ACTIVE }
                ) {
                    allActive = true
                    LOG.info("All indexes are now active")
                } else {
                    val pendingIndexes =
                        gsiStatuses
                            .filter { it.value != software.amazon.awssdk.services.dynamodb.model.IndexStatus.ACTIVE }
                            .keys
                            .joinToString(", ")
                    LOG.info("Waiting for indexes to become active: $pendingIndexes")
                }
            } catch (e: Exception) {
                LOG.warn("Error checking index status", e)
            }

            attempts++
        }

        if (!allActive) {
            LOG.warn("Timed out waiting for indexes to become active")
        }
    }

    fun createTable() {
        val attributeDefinitions =
            mutableListOf(
                AttributeDefinition
                    .builder()
                    .attributeName(ATTRIBUTE_PK)
                    .attributeType(ScalarAttributeType.S)
                    .build(),
                AttributeDefinition
                    .builder()
                    .attributeName(ATTRIBUTE_SK)
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )

        SUPPORTED_INDEXES.forEach { index ->
            attributeDefinitions.add(
                AttributeDefinition
                    .builder()
                    .attributeName(index.partitionKeyName)
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )
            attributeDefinitions.add(
                AttributeDefinition
                    .builder()
                    .attributeName(index.sortKeyName)
                    .attributeType(ScalarAttributeType.S)
                    .build(),
            )
        }

        val globalSecondaryIndexes =
            SUPPORTED_INDEXES.map {
                if (it.indexName == INDEX_GSI_1) gsi1() else buildGlobalSecondaryIndex(it)
            }

        dynamoDbClient.createTable(
            CreateTableRequest
                .builder()
                .attributeDefinitions(attributeDefinitions)
                .keySchema(
                    Arrays.asList(
                        KeySchemaElement
                            .builder()
                            .attributeName(ATTRIBUTE_PK)
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement
                            .builder()
                            .attributeName(ATTRIBUTE_SK)
                            .keyType(KeyType.RANGE)
                            .build(),
                    ),
                ).billingMode(BillingMode.PAY_PER_REQUEST)
                .tableName(dynamoConfiguration.tableName)
                .globalSecondaryIndexes(globalSecondaryIndexes)
                .build(),
        )
    }

    @NonNull
    fun findAllQueryRequest(
        @NonNull cls: Class<*>,
        @Nullable beforeId: String?,
        @Nullable limit: Int?,
    ): QueryRequest {
        val builder =
            QueryRequest
                .builder()
                .tableName(dynamoConfiguration.tableName)
                .indexName(INDEX_GSI_1)
                .scanIndexForward(false)

        if (limit != null) {
            builder.limit(limit)
        }

        return if (beforeId == null) {
            builder
                .keyConditionExpression("#pk = :pk")
                .expressionAttributeNames(Collections.singletonMap("#pk", ATTRIBUTE_GSI_1_PK))
                .expressionAttributeValues(
                    Collections.singletonMap(
                        ":pk",
                        classAttributeValue(cls),
                    ),
                ).build()
        } else {
            builder
                .keyConditionExpression("#pk = :pk and #sk < :sk")
                .expressionAttributeNames(
                    mapOf(
                        "#pk" to ATTRIBUTE_GSI_1_PK,
                        "#sk" to ATTRIBUTE_GSI_1_SK,
                    ),
                ).expressionAttributeValues(
                    mapOf(
                        ":pk" to classAttributeValue(cls),
                        ":sk" to id(cls, beforeId),
                    ),
                ).build()
        }
    }

    protected open fun delete(
        @NonNull @NotNull cls: Class<*>,
        @NonNull @NotBlank id: String,
    ) {
        val pk = id(cls, id)
        val deleteItemResponse =
            dynamoDbClient.deleteItem(
                DeleteItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .key(mapOf(ATTRIBUTE_PK to pk, ATTRIBUTE_SK to pk))
                    .build(),
            )
        if (LOG.isDebugEnabled) {
            LOG.debug(deleteItemResponse.toString())
        }
    }

    protected open fun findById(
        @NonNull @NotNull cls: Class<*>,
        @NonNull @NotBlank id: String,
    ): (Map<String, AttributeValue>)? {
        val pk = id(cls, id)
        val getItemResponse =
            dynamoDbClient.getItem(
                GetItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .key(mapOf(ATTRIBUTE_PK to pk, ATTRIBUTE_SK to pk))
                    .build(),
            )
        return if (!getItemResponse.hasItem()) null else getItemResponse.item()
    }

    @NonNull
    protected open fun item(
        @NonNull entity: T,
    ): Map<String, AttributeValue> {
        val item = HashMap<String, AttributeValue>()
        val pk = id(entity.javaClass, entity.id ?: "")
        item[ATTRIBUTE_PK] = pk
        item[ATTRIBUTE_SK] = pk
        item[ATTRIBUTE_GSI_1_PK] = classAttributeValue(entity.javaClass)
        item[ATTRIBUTE_GSI_1_SK] = pk

        if (entity is Indexable) {
            val indexValues = entity.getIndexValues()
            for ((key, value) in indexValues) {
                item[key] = AttributeValue.builder().s(value).build()
            }
        }

        return item
    }

    fun <E : Identified> createIndexQuery(
        indexName: String,
        partitionKeyName: String,
        partitionKeyValue: String,
        sortKeyName: String? = null,
        sortKeyValue: String? = null,
        sortKeyOperator: String = "=",
    ): QueryRequest {
        val builder =
            QueryRequest
                .builder()
                .tableName(dynamoConfiguration.tableName)
                .indexName(indexName)

        if (sortKeyName != null && sortKeyValue != null) {
            builder
                .keyConditionExpression("#pk = :pk and #sk $sortKeyOperator :sk")
                .expressionAttributeNames(
                    mapOf(
                        "#pk" to partitionKeyName,
                        "#sk" to sortKeyName,
                    ),
                ).expressionAttributeValues(
                    mapOf(
                        ":pk" to AttributeValue.builder().s(partitionKeyValue).build(),
                        ":sk" to AttributeValue.builder().s(sortKeyValue).build(),
                    ),
                )
        } else {
            builder
                .keyConditionExpression("#pk = :pk")
                .expressionAttributeNames(mapOf("#pk" to partitionKeyName))
                .expressionAttributeValues(
                    mapOf(":pk" to AttributeValue.builder().s(partitionKeyValue).build()),
                )
        }

        return builder.build()
    }

    fun getTableName(): String = dynamoConfiguration.tableName
}
