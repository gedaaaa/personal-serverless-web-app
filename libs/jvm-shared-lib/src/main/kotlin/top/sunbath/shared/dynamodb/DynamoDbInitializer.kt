package top.sunbath.shared.dynamodb

import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

/**
 * Initializes DynamoDB tables and indexes on application startup.
 * This class is annotated with @Context to ensure it's loaded eagerly at startup.
 */
@Context
@Singleton
@Requires(property = "my-property.custom-dynamodb-initialization.enabled", value = "true")
class DynamoDbInitializer(
    private val dynamoDbClient: DynamoDbClient,
) : ApplicationEventListener<StartupEvent> {
    companion object {
        private val LOG = LoggerFactory.getLogger(DynamoDbInitializer::class.java)
        private const val TABLE_CREATION_LOCK_PREFIX = "table_creation_lock_"
        private const val INDEX_CREATION_LOCK_PREFIX = "index_creation_lock_"
        private const val LOCK_TIMEOUT_SECONDS = 600L // 10 minutes
    }

    @Inject
    private lateinit var repositories: List<DynamoRepository<*>>

    override fun onApplicationEvent(event: StartupEvent) {
        LOG.info("Initializing DynamoDB tables and indexes")

        // Create a distributed lock
        val lock = DynamoDbLock(dynamoDbClient, lockTimeoutSeconds = LOCK_TIMEOUT_SECONDS)

        repositories.forEach { repository ->
            val tableName = repository.getTableName()

            try {
                // First check if the table exists
                val tableExists = repository.existsTable()

                if (!tableExists) {
                    // Try to acquire a lock for table creation
                    val tableCreationLockId = TABLE_CREATION_LOCK_PREFIX + tableName

                    LOG.info("Table $tableName does not exist, attempting to acquire lock for creation")

                    val acquired =
                        lock.withLock(tableCreationLockId) {
                            // Check again in case another instance created the table while we were acquiring the lock
                            if (!repository.existsTable()) {
                                LOG.info("Creating table $tableName")
                                repository.createTable()
                                LOG.info("Table $tableName created successfully")
                            } else {
                                LOG.info("Table $tableName was created by another instance while acquiring lock")
                            }
                        }

                    if (!acquired) {
                        LOG.info("Could not acquire lock for table creation, another instance may be creating the table")
                    }
                } else {
                    // Table exists, check if indexes need to be updated
                    val indexCreationLockId = INDEX_CREATION_LOCK_PREFIX + tableName

                    LOG.info("Table $tableName exists, checking if indexes need to be updated")

                    // Try to acquire a lock for index creation
                    val acquired =
                        lock.withLock(indexCreationLockId) {
                            LOG.info("Updating indexes for table $tableName")
                            repository.updateTableIndexes()
                        }

                    if (!acquired) {
                        LOG.info("Could not acquire lock for index creation, another instance may be updating indexes")
                    }
                }
            } catch (e: Exception) {
                LOG.error("Error initializing table: $tableName", e)
                // Don't throw the exception to allow the application to start
                // even if table initialization fails
            }
        }

        LOG.info("DynamoDB initialization completed")
    }
}
