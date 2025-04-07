package top.sunbath.shared.dynamodb

import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import java.time.Instant
import java.util.UUID

/**
 * A distributed lock implementation using DynamoDB.
 * This class provides a way to acquire and release locks across multiple service instances.
 */
class DynamoDbLock(
    private val dynamoDbClient: DynamoDbClient,
    private val lockTableName: String = "distributed_locks",
    private val lockTimeoutSeconds: Long = 300, // 5 minutes
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(DynamoDbLock::class.java)
        private const val LOCK_ID = "lockId"
        private const val OWNER = "owner"
        private const val EXPIRATION = "expiration"
        private const val LOCK_DATA = "lockData"
    }

    private val instanceId = UUID.randomUUID().toString()

    /**
     * Ensures the lock table exists.
     */
    fun ensureLockTableExists() {
        try {
            dynamoDbClient.describeTable { it.tableName(lockTableName) }
            LOG.debug("Lock table $lockTableName already exists")
        } catch (e: ResourceNotFoundException) {
            try {
                LOG.info("Creating lock table $lockTableName")
                dynamoDbClient.createTable(
                    CreateTableRequest
                        .builder()
                        .tableName(lockTableName)
                        .keySchema(
                            KeySchemaElement
                                .builder()
                                .attributeName(LOCK_ID)
                                .keyType(KeyType.HASH)
                                .build(),
                        ).attributeDefinitions(
                            AttributeDefinition
                                .builder()
                                .attributeName(LOCK_ID)
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        ).billingMode(BillingMode.PAY_PER_REQUEST)
                        .build(),
                )
                LOG.info("Lock table $lockTableName created successfully")
            } catch (ex: ResourceInUseException) {
                // Table is being created by another instance
                LOG.info("Lock table $lockTableName is being created by another instance")
            }
        }
    }

    /**
     * Tries to acquire a lock with the given ID.
     *
     * @param lockId The ID of the lock to acquire
     * @param data Optional data to store with the lock
     * @return true if the lock was acquired, false otherwise
     */
    fun tryAcquireLock(
        lockId: String,
        data: String? = null,
    ): Boolean {
        ensureLockTableExists()

        val now = Instant.now()
        val expirationTime = now.plusSeconds(lockTimeoutSeconds)

        try {
            // First, try to get the current lock to see if it exists and is expired
            val getItemResponse =
                dynamoDbClient.getItem(
                    GetItemRequest
                        .builder()
                        .tableName(lockTableName)
                        .key(mapOf(LOCK_ID to AttributeValue.builder().s(lockId).build()))
                        .build(),
                )

            if (getItemResponse.hasItem()) {
                val item = getItemResponse.item()
                val expiration = Instant.ofEpochMilli(item[EXPIRATION]!!.n().toLong())
                val owner = item[OWNER]!!.s()

                // If the lock is owned by this instance, refresh it
                if (owner == instanceId) {
                    updateLock(lockId, expirationTime, data)
                    return true
                }

                // If the lock is expired, try to acquire it
                if (now.isAfter(expiration)) {
                    return tryAcquireExpiredLock(lockId, expirationTime, data)
                }

                // Lock exists and is not expired
                return false
            } else {
                // Lock doesn't exist, try to create it
                return tryCreateNewLock(lockId, expirationTime, data)
            }
        } catch (e: Exception) {
            LOG.error("Error acquiring lock $lockId", e)
            return false
        }
    }

    private fun tryAcquireExpiredLock(
        lockId: String,
        expirationTime: Instant,
        data: String?,
    ): Boolean {
        try {
            // Try to update the lock with a condition that it's expired
            val now = Instant.now()
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(lockTableName)
                    .item(createLockItem(lockId, expirationTime, data))
                    .conditionExpression("attribute_exists(#lockId) AND #expiration < :now")
                    .expressionAttributeNames(
                        mapOf(
                            "#lockId" to LOCK_ID,
                            "#expiration" to EXPIRATION,
                        ),
                    ).expressionAttributeValues(
                        mapOf(
                            ":now" to AttributeValue.builder().n(now.toEpochMilli().toString()).build(),
                        ),
                    ).build(),
            )
            LOG.debug("Acquired expired lock $lockId")
            return true
        } catch (e: ConditionalCheckFailedException) {
            // Lock is not expired or was acquired by another instance
            LOG.debug("Failed to acquire expired lock $lockId: it was acquired by another instance or is not expired")
            return false
        } catch (e: Exception) {
            LOG.error("Error acquiring expired lock $lockId", e)
            return false
        }
    }

    private fun tryCreateNewLock(
        lockId: String,
        expirationTime: Instant,
        data: String?,
    ): Boolean {
        try {
            // Try to create a new lock with a condition that it doesn't exist
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(lockTableName)
                    .item(createLockItem(lockId, expirationTime, data))
                    .conditionExpression("attribute_not_exists(#lockId)")
                    .expressionAttributeNames(
                        mapOf("#lockId" to LOCK_ID),
                    ).build(),
            )
            LOG.debug("Created new lock $lockId")
            return true
        } catch (e: ConditionalCheckFailedException) {
            // Lock already exists
            LOG.debug("Failed to create new lock $lockId: it already exists")
            return false
        } catch (e: Exception) {
            LOG.error("Error creating new lock $lockId", e)
            return false
        }
    }

    private fun updateLock(
        lockId: String,
        expirationTime: Instant,
        data: String?,
    ) {
        try {
            dynamoDbClient.putItem(
                PutItemRequest
                    .builder()
                    .tableName(lockTableName)
                    .item(createLockItem(lockId, expirationTime, data))
                    .conditionExpression("#owner = :owner")
                    .expressionAttributeNames(
                        mapOf("#owner" to OWNER),
                    ).expressionAttributeValues(
                        mapOf(":owner" to AttributeValue.builder().s(instanceId).build()),
                    ).build(),
            )
            LOG.debug("Updated lock $lockId")
        } catch (e: Exception) {
            LOG.error("Error updating lock $lockId", e)
        }
    }

    private fun createLockItem(
        lockId: String,
        expirationTime: Instant,
        data: String?,
    ): Map<String, AttributeValue> {
        val item =
            mutableMapOf(
                LOCK_ID to AttributeValue.builder().s(lockId).build(),
                OWNER to AttributeValue.builder().s(instanceId).build(),
                EXPIRATION to AttributeValue.builder().n(expirationTime.toEpochMilli().toString()).build(),
            )

        if (data != null) {
            item[LOCK_DATA] = AttributeValue.builder().s(data).build()
        }

        return item
    }

    /**
     * Releases a lock if it is owned by this instance.
     *
     * @param lockId The ID of the lock to release
     * @return true if the lock was released, false otherwise
     */
    fun releaseLock(lockId: String): Boolean {
        try {
            dynamoDbClient.deleteItem(
                DeleteItemRequest
                    .builder()
                    .tableName(lockTableName)
                    .key(mapOf(LOCK_ID to AttributeValue.builder().s(lockId).build()))
                    .conditionExpression("#owner = :owner")
                    .expressionAttributeNames(
                        mapOf("#owner" to OWNER),
                    ).expressionAttributeValues(
                        mapOf(":owner" to AttributeValue.builder().s(instanceId).build()),
                    ).build(),
            )
            LOG.debug("Released lock $lockId")
            return true
        } catch (e: ConditionalCheckFailedException) {
            // Lock is not owned by this instance
            LOG.debug("Failed to release lock $lockId: it is not owned by this instance")
            return false
        } catch (e: Exception) {
            LOG.error("Error releasing lock $lockId", e)
            return false
        }
    }

    /**
     * Executes the given action with a lock.
     * If the lock cannot be acquired, the action is not executed.
     *
     * @param lockId The ID of the lock to acquire
     * @param data Optional data to store with the lock
     * @param action The action to execute with the lock
     * @return true if the action was executed, false otherwise
     */
    fun withLock(
        lockId: String,
        data: String? = null,
        action: () -> Unit,
    ): Boolean {
        if (tryAcquireLock(lockId, data)) {
            try {
                action()
                return true
            } finally {
                releaseLock(lockId)
            }
        }
        return false
    }
}
