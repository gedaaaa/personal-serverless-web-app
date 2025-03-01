package top.sunbath.api.auth.dynamodbUtil

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

/**
 * Factory for creating DynamoDB clients based on the environment.
 */
@Factory
class DynamoDbClientFactory {
    private val log = LoggerFactory.getLogger(DynamoDbClientFactory::class.java)

    /**
     * Creates a DynamoDB client for development environments.
     * This uses a local DynamoDB endpoint.
     */
    @Singleton
    @Requires(notEnv = ["production"])
    @Requires(property = "dynamodb-local.host")
    @Requires(property = "dynamodb-local.port")
    fun devDynamoDbClient(
        @Property(name = "dynamodb-local.host") host: String,
        @Property(name = "dynamodb-local.port") port: String,
        @Property(name = "aws.region") regionName: String,
        credentialsProvider: AwsCredentialsProvider,
    ): DynamoDbClient {
        log.info("Creating DynamoDB client for development environment with endpoint: http://$host:$port")

        return DynamoDbClient
            .builder()
            .endpointOverride(URI.create("http://$host:$port"))
            .region(Region.of(regionName))
            .credentialsProvider(credentialsProvider)
            .build()
    }

    /**
     * Creates a DynamoDB client for production environments.
     * This uses the AWS region from configuration and the default credentials provider chain.
     */
    @Singleton
    @Requires(env = ["production"])
    @Requires(property = "aws.region")
    fun prodDynamoDbClient(
        @Property(name = "aws.region") regionName: String,
        credentialsProvider: AwsCredentialsProvider,
    ): DynamoDbClient {
        log.info("Creating DynamoDB client for production environment in region: $regionName")

        return DynamoDbClient
            .builder()
            .region(Region.of(regionName))
            .credentialsProvider(credentialsProvider)
            .build()
    }
} 
