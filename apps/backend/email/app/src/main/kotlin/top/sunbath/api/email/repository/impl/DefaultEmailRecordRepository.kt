package top.sunbath.api.email.repository.impl

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
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
import top.sunbath.api.email.model.EmailRecord
import top.sunbath.api.email.repository.EmailRecordRepository
import top.sunbath.shared.dynamodb.DynamoConfiguration
import top.sunbath.shared.dynamodb.DynamoRepository
import top.sunbath.shared.dynamodb.IdGenerator
import top.sunbath.shared.dynamodb.IndexDefinition

@Singleton
open class DefaultEmailRecordRepository(
    dynamoDbClient: DynamoDbClient,
    dynamoConfiguration: DynamoConfiguration,
    private val idGenerator: IdGenerator,
) : DynamoRepository<EmailRecord>(dynamoDbClient, dynamoConfiguration),
    EmailRecordRepository {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DefaultEmailRecordRepository::class.java)
        private const val ATTRIBUTE_ID = "id"
        private const val ATTRIBUTE_TO = "to"
        private const val ATTRIBUTE_FROM = "from"
        private const val ATTRIBUTE_SUBJECT = "subject"
        private const val ATTRIBUTE_HTML = "html"
        private const val ATTRIBUTE_VENDOR_RESPONSE = "vendorResponse"

        // Define index constants
        private const val TO_INDEX = "TO_INDEX"
        private const val TO_PK = "TO_PK"
        private const val TO_SK = "TO_SK"

        // Register indexes
        init {
            DynamoRepository.registerIndex(IndexDefinition(TO_INDEX, TO_PK, TO_SK))
        }
    }

    // Add a constructor init block to ensure indexes are registered
    init {
        // This ensures that the companion object's init block is executed
        // and the indexes are registered before the repository is used
        LOG.debug("Initializing DefaultEmailRecordRepository with to index: $TO_INDEX")
    }

    @NonNull
    override fun save(
        @NonNull @NotBlank to: String,
        @NonNull @NotBlank from: String,
        @NonNull @NotBlank subject: String,
        @NonNull @NotBlank html: String,
        @NonNull vendorResponse: String,
    ): String {
        val id = idGenerator.generate()
        save(
            EmailRecord(
                id = id,
                to = to,
                from = from,
                subject = subject,
                html = html,
                vendorResponse = vendorResponse,
            ),
        )
        return id
    }

    protected open fun save(
        @NonNull @NotNull @Valid emailRecord: EmailRecord,
    ) {
        val itemResponse: PutItemResponse =
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .item(item(emailRecord))
                    .build(),
            )
        if (LOG.isDebugEnabled) {
            LOG.debug(itemResponse.toString())
        }
    }

    @Nullable
    override fun findById(id: String): EmailRecord? {
        return findById(EmailRecord::class.java, id)?.let { return emailRecordOf(it) }
    }

    @NonNull
    override fun item(
        @NonNull entity: EmailRecord,
    ): Map<String, AttributeValue> {
        val result = super.item(entity).toMutableMap()
        result[ATTRIBUTE_ID] = AttributeValue.builder().s(entity.id).build()
        result[ATTRIBUTE_TO] = AttributeValue.builder().s(entity.to).build()
        result[ATTRIBUTE_FROM] = AttributeValue.builder().s(entity.from).build()
        result[ATTRIBUTE_SUBJECT] = AttributeValue.builder().s(entity.subject).build()
        result[ATTRIBUTE_HTML] = AttributeValue.builder().s(entity.html).build()
        result[ATTRIBUTE_VENDOR_RESPONSE] = AttributeValue.builder().s(entity.vendorResponse).build()

        return result
    }

    private fun emailRecordOf(item: Map<String, AttributeValue>): EmailRecord =
        EmailRecord(
            id = item[ATTRIBUTE_ID]?.s() ?: throw IllegalArgumentException("id is required"),
            to = item[ATTRIBUTE_TO]?.s() ?: throw IllegalArgumentException("to is required"),
            from = item[ATTRIBUTE_FROM]?.s() ?: throw IllegalArgumentException("from is required"),
            subject = item[ATTRIBUTE_SUBJECT]?.s() ?: throw IllegalArgumentException("subject is required"),
            html = item[ATTRIBUTE_HTML]?.s() ?: throw IllegalArgumentException("html is required"),
            vendorResponse = item[ATTRIBUTE_VENDOR_RESPONSE]?.s() ?: throw IllegalArgumentException("vendorResponse is required"),
        )
}
