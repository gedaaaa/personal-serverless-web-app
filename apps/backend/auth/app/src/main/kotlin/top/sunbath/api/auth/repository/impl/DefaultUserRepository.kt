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
import top.sunbath.api.auth.model.PasswordType
import top.sunbath.api.auth.model.User
import top.sunbath.api.auth.repository.UserRepository
import top.sunbath.shared.dynamodb.DynamoConfiguration
import top.sunbath.shared.dynamodb.DynamoRepository
import top.sunbath.shared.dynamodb.IdGenerator
import top.sunbath.shared.dynamodb.IndexDefinition
import java.time.Instant

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
        private const val ATTRIBUTE_PASSWORD_TYPE = "passwordType"
        private const val ATTRIBUTE_MIGRATION_TOKEN = "migrationToken"
        private const val ATTRIBUTE_MIGRATION_TOKEN_EXPIRES_AT = "migrationTokenExpiresAt"
        private const val ATTRIBUTE_ROLES = "roles"
        private const val ATTRIBUTE_FULLNAME = "fullName"
        private const val ATTRIBUTE_EMAIL_VERIFIED = "emailVerified"
        private const val ATTRIBUTE_EMAIL_VERIFICATION_TOKEN = "emailVerificationToken"
        private const val ATTRIBUTE_EMAIL_VERIFICATION_TOKEN_EXPIRES_AT = "emailVerificationTokenExpiresAt"
        private const val ATTRIBUTE_LAST_VERIFICATION_EMAIL_SENT_AT = "lastVerificationEmailSentAt"

        // Define index constants
        private const val USERNAME_INDEX = "USERNAME_INDEX"
        private const val USERNAME_PK = "USERNAME_PK"
        private const val USERNAME_SK = "USERNAME_SK"
        private const val EMAIL_INDEX = "EMAIL_INDEX"
        private const val EMAIL_PK = "EMAIL_PK"
        private const val EMAIL_SK = "EMAIL_SK"
        private const val VERIFICATION_TOKEN_INDEX = "VERIFICATION_TOKEN_INDEX"
        private const val VERIFICATION_TOKEN_PK = "VERIFICATION_TOKEN_PK"
        private const val VERIFICATION_TOKEN_SK = "VERIFICATION_TOKEN_SK"

        // Register indexes
        init {
            DynamoRepository.registerIndex(IndexDefinition(USERNAME_INDEX, USERNAME_PK, USERNAME_SK))
            DynamoRepository.registerIndex(IndexDefinition(EMAIL_INDEX, EMAIL_PK, EMAIL_SK))
            DynamoRepository.registerIndex(IndexDefinition(VERIFICATION_TOKEN_INDEX, VERIFICATION_TOKEN_PK, VERIFICATION_TOKEN_SK))
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
        emailVerified: Boolean,
        emailVerificationToken: String?,
        emailVerificationTokenExpiresAt: Instant?,
        lastVerificationEmailSentAt: Instant?,
    ): String {
        val id = idGenerator.generate()
        save(
            User(
                id = id,
                username = username,
                email = email,
                password = password,
                passwordType = PasswordType.V2, // new user should use new version of password type
                migrationToken = null,
                migrationTokenExpiresAt = null,
                roles = roles,
                fullName = fullName,
                emailVerified = emailVerified,
                emailVerificationToken = emailVerificationToken,
                emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt,
                lastVerificationEmailSentAt = lastVerificationEmailSentAt,
            ),
        )
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

    @NonNull
    override fun findByEmail(
        @NonNull @NotBlank email: String,
    ): User? {
        // Use the generic index query method
        val queryRequest =
            createIndexQuery<User>(
                indexName = EMAIL_INDEX,
                partitionKeyName = EMAIL_PK,
                partitionKeyValue = email,
            )

        val response = dynamoDbClient.query(queryRequest)
        return if (response.items().isEmpty()) null else userOf(response.items()[0])
    }

    @NonNull
    override fun findByVerificationToken(
        @NonNull @NotBlank token: String,
    ): User? {
        // Use the generic index query method
        val queryRequest =
            createIndexQuery<User>(
                indexName = VERIFICATION_TOKEN_INDEX,
                partitionKeyName = VERIFICATION_TOKEN_PK,
                partitionKeyValue = token,
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
        emailVerified: Boolean?,
        emailVerificationToken: String?,
        emailVerificationTokenExpiresAt: Instant?,
        lastVerificationEmailSentAt: Instant?,
    ): Boolean {
        val existingUser = findById(id) ?: return false

        // Update only the non-null fields
        email?.let { existingUser.email = it }
        password?.let { existingUser.password = it }
        roles?.let { existingUser.roles = it }
        // fullName can be set to null explicitly
        existingUser.fullName = fullName
        emailVerified?.let { existingUser.emailVerified = it }
        existingUser.emailVerificationToken = emailVerificationToken
        existingUser.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt
        existingUser.lastVerificationEmailSentAt = lastVerificationEmailSentAt

        // Save the updated user
        save(existingUser)
        return true
    }

    /**
     * Update user's password related fields for migration.
     */
    override fun updatePasswordSettings(
        @NonNull @NotBlank id: String,
        password: String?,
        passwordType: PasswordType?,
        migrationToken: String?,
        migrationTokenExpiresAt: Instant?,
    ): Boolean {
        val existingUser = findById(id) ?: return false

        // Update only the non-null fields
        password?.let { existingUser.password = it }
        passwordType?.let { existingUser.passwordType = it }
        existingUser.migrationToken = migrationToken
        existingUser.migrationTokenExpiresAt = migrationTokenExpiresAt

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
            beforeId = lastEvaluatedId(response, User::class.java)
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
        val nextCursor = lastEvaluatedId(response, User::class.java)

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
            id = item[ATTRIBUTE_ID]!!.s(),
            username = item[ATTRIBUTE_USERNAME]!!.s(),
            email = item[ATTRIBUTE_EMAIL]!!.s(),
            password = item[ATTRIBUTE_PASSWORD]!!.s(),
            // default to V1 if password type is not set (possibly legacy users)
            passwordType =
                item[ATTRIBUTE_PASSWORD_TYPE]?.s()?.let {
                    try {
                        PasswordType.valueOf(it)
                    } catch (e: Exception) {
                        PasswordType.V1
                    }
                } ?: PasswordType.V1,
            migrationToken = item[ATTRIBUTE_MIGRATION_TOKEN]?.s(),
            migrationTokenExpiresAt = item[ATTRIBUTE_MIGRATION_TOKEN_EXPIRES_AT]?.s()?.let { Instant.parse(it) },
            roles = item[ATTRIBUTE_ROLES]?.ss()?.toSet() ?: setOf("ROLE_USER"),
            fullName = item[ATTRIBUTE_FULLNAME]?.s(),
            emailVerified = item[ATTRIBUTE_EMAIL_VERIFIED]?.bool() ?: false,
            emailVerificationToken = item[ATTRIBUTE_EMAIL_VERIFICATION_TOKEN]?.s(),
            emailVerificationTokenExpiresAt = item[ATTRIBUTE_EMAIL_VERIFICATION_TOKEN_EXPIRES_AT]?.s()?.let { Instant.parse(it) },
            lastVerificationEmailSentAt = item[ATTRIBUTE_LAST_VERIFICATION_EMAIL_SENT_AT]?.s()?.let { Instant.parse(it) },
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
        result[ATTRIBUTE_PASSWORD_TYPE] = AttributeValue.builder().s(entity.passwordType.name).build()
        entity.migrationToken?.let {
            result[ATTRIBUTE_MIGRATION_TOKEN] = AttributeValue.builder().s(it).build()
        }
        entity.migrationTokenExpiresAt?.let {
            result[ATTRIBUTE_MIGRATION_TOKEN_EXPIRES_AT] = AttributeValue.builder().s(it.toString()).build()
        }
        result[ATTRIBUTE_ROLES] = AttributeValue.builder().ss(entity.roles).build()
        entity.fullName?.let {
            result[ATTRIBUTE_FULLNAME] = AttributeValue.builder().s(it).build()
        }
        result[ATTRIBUTE_EMAIL_VERIFIED] = AttributeValue.builder().bool(entity.emailVerified).build()
        entity.emailVerificationToken?.let {
            result[ATTRIBUTE_EMAIL_VERIFICATION_TOKEN] = AttributeValue.builder().s(it).build()
        }
        entity.emailVerificationTokenExpiresAt?.let {
            result[ATTRIBUTE_EMAIL_VERIFICATION_TOKEN_EXPIRES_AT] = AttributeValue.builder().s(it.toString()).build()
        }
        entity.lastVerificationEmailSentAt?.let {
            result[ATTRIBUTE_LAST_VERIFICATION_EMAIL_SENT_AT] = AttributeValue.builder().s(it.toString()).build()
        }

        return result
    }
}
