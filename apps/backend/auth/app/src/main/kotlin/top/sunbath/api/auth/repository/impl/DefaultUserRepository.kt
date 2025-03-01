package top.sunbath.api.auth.repository.impl

import io.micronaut.core.annotation.NonNull
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
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import top.sunbath.api.auth.config.DynamoConfiguration
import top.sunbath.api.auth.dynamodbUtil.DynamoRepository
import top.sunbath.api.auth.dynamodbUtil.IdGenerator
import top.sunbath.api.auth.model.User
import top.sunbath.api.auth.repository.UserRepository

@Singleton
open class DefaultUserRepository(
    dynamoDbClient: DynamoDbClient,
    dynamoConfiguration: DynamoConfiguration,
    private val idGenerator: IdGenerator,
) : DynamoRepository<User>(dynamoDbClient, dynamoConfiguration),
    UserRepository {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DefaultUserRepository::class.java)
        private const val ATTRIBUTE_ID = "id"
        private const val ATTRIBUTE_USERNAME = "username"
        private const val ATTRIBUTE_EMAIL = "email"
        private const val ATTRIBUTE_PASSWORD = "password"
        private const val ATTRIBUTE_ROLES = "roles"
        private const val ATTRIBUTE_FULLNAME = "fullName"
    }

    @NonNull
    override fun save(
        @NonNull @NotBlank username: String,
        @NonNull @NotBlank email: String,
        @NonNull @NotBlank password: String,
        @NonNull roles: Set<String>,
        fullName: String?,
    ): String {
        val id = idGenerator.generate()
        save(User(id, username, email, password, roles, fullName))
        return id
    }

    protected open fun save(
        @NonNull @NotNull @Valid user: User,
    ) {
        val itemResponse: PutItemResponse =
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .item(item(user))
                    .build(),
            )
        if (LOG.isDebugEnabled) {
            LOG.debug(itemResponse.toString())
        }
    }

    @NonNull
    override fun findById(
        @NonNull @NotBlank id: String,
    ): User? {
        return findById(User::class.java, id)?.let { return userOf(it) }
    }

    @NonNull
    override fun findByUsername(
        @NonNull @NotBlank username: String,
    ): User? {
        val scanRequest =
            ScanRequest
                .builder()
                .tableName(dynamoConfiguration.tableName)
                .filterExpression("#username = :username")
                .expressionAttributeNames(mapOf("#username" to ATTRIBUTE_USERNAME))
                .expressionAttributeValues(mapOf(":username" to AttributeValue.builder().s(username).build()))
                .build()

        val response = dynamoDbClient.scan(scanRequest)
        return if (response.items().isEmpty()) null else userOf(response.items()[0])
    }

    override fun delete(
        @NonNull @NotBlank id: String,
    ) {
        delete(User::class.java, id)
    }

    @NonNull
    override fun findAll(): List<User> {
        val result = ArrayList<User>()
        var beforeId: String? = null
        do {
            val request: QueryRequest = findAllQueryRequest(User::class.java, beforeId, null)
            val response: QueryResponse = dynamoDbClient.query(request)
            if (LOG.isTraceEnabled) {
                LOG.trace(response.toString())
            }
            result.addAll(parseInResponse(response))
            beforeId = lastEvaluatedId(response, User::class.java).orElse(null)
        } while (beforeId != null)
        return result
    }

    private fun parseInResponse(response: QueryResponse): List<User> {
        val items: List<Map<String, AttributeValue>> = response.items()
        val result = ArrayList<User>()
        if (CollectionUtils.isNotEmpty(items)) {
            for (item in items) {
                result.add(userOf(item))
            }
        }
        return result
    }

    @NonNull
    private fun userOf(
        @NonNull item: Map<String, AttributeValue>,
    ): User =
        User(
            item[ATTRIBUTE_ID]!!.s(),
            item[ATTRIBUTE_USERNAME]!!.s(),
            item[ATTRIBUTE_EMAIL]!!.s(),
            item[ATTRIBUTE_PASSWORD]!!.s(),
            item[ATTRIBUTE_ROLES]?.ss()?.toSet() ?: setOf("ROLE_USER"),
            item[ATTRIBUTE_FULLNAME]?.s(),
        )

    @NonNull
    override fun item(
        @NonNull entity: User,
    ): Map<String, AttributeValue> {
        val result = super.item(entity).toMutableMap()
        result[ATTRIBUTE_ID] = AttributeValue.builder().s(entity.id).build()
        result[ATTRIBUTE_USERNAME] = AttributeValue.builder().s(entity.username).build()
        result[ATTRIBUTE_EMAIL] = AttributeValue.builder().s(entity.email).build()
        result[ATTRIBUTE_PASSWORD] = AttributeValue.builder().s(entity.password).build()
        result[ATTRIBUTE_ROLES] = AttributeValue.builder().ss(entity.roles).build()
        entity.fullName?.let {
            result[ATTRIBUTE_FULLNAME] = AttributeValue.builder().s(it).build()
        }
        return result
    }
}
