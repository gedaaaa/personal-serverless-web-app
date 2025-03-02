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
import top.sunbath.api.auth.config.DynamoConfiguration
import top.sunbath.api.auth.dynamodbUtil.DynamoRepository
import top.sunbath.api.auth.dynamodbUtil.IdGenerator
import top.sunbath.api.auth.dynamodbUtil.IndexDefinition
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

        // Define index constants
        private const val USERNAME_INDEX = "USERNAME_INDEX"
        private const val USERNAME_PK = "USERNAME_PK"
        private const val USERNAME_SK = "USERNAME_SK"

        // Register the username index
        init {
            DynamoRepository.registerIndex(IndexDefinition(USERNAME_INDEX, USERNAME_PK, USERNAME_SK))
        }
    }

    // Add a constructor init block to ensure indexes are registered
    init {
        // This ensures that the companion object's init block is executed
        // and the indexes are registered before the repository is used
        LOG.debug("Initializing DefaultUserRepository with username index: $USERNAME_INDEX")
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
        // Use the generic index query method
        val queryRequest =
            createIndexQuery<User>(
                indexName = USERNAME_INDEX,
                partitionKeyName = USERNAME_PK,
                partitionKeyValue = username,
            )

        val response = dynamoDbClient.query(queryRequest)
        return if (response.items().isEmpty()) null else userOf(response.items()[0])
    }

    override fun delete(
        @NonNull @NotBlank id: String,
    ) {
        delete(User::class.java, id)
    }

    override fun update(
        @NonNull @NotBlank id: String,
        email: String?,
        password: String?,
        roles: Set<String>?,
        fullName: String?,
    ): Boolean {
        val existingUser = findById(id) ?: return false

        // Update only the non-null fields
        email?.let { existingUser.email = it }
        password?.let { existingUser.password = it }
        roles?.let { existingUser.roles = it }
        // fullName can be set to null explicitly
        existingUser.fullName = fullName

        // Save the updated user
        save(existingUser)
        return true
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

    @NonNull
    override fun findAllWithCursor(
        limit: Int,
        lastEvaluatedId: String?,
    ): Pair<List<User>, String?> {
        if (limit <= 0) {
            return Pair(emptyList(), null)
        }

        val request: QueryRequest = findAllQueryRequest(User::class.java, lastEvaluatedId, limit)
        val response: QueryResponse = dynamoDbClient.query(request)

        if (LOG.isTraceEnabled) {
            LOG.trace(response.toString())
        }

        val users = parseInResponse(response)
        val nextCursor = lastEvaluatedId(response, User::class.java).orElse(null)

        return Pair(users, nextCursor)
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
