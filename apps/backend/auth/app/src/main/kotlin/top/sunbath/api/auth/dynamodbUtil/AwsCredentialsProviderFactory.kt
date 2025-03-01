package top.sunbath.api.auth.dynamodbUtil

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

/**
 * Factory for creating AWS credentials providers based on the environment.
 * In development, it uses static credentials from configuration.
 * In production, it uses the default AWS credentials provider chain.
 */
@Factory
class AwsCredentialsProviderFactory {
    private val log = LoggerFactory.getLogger(AwsCredentialsProviderFactory::class.java)

    /**
     * Creates an AWS credentials provider for development environments.
     * This uses static credentials from configuration.
     */
    @Singleton
    @Requires(notEnv = ["production"])
    @Requires(property = "aws.access-key-id")
    @Requires(property = "aws.secret-access-key")
    fun devCredentialsProvider(
        @Property(name = "aws.access-key-id") accessKeyId: String,
        @Property(name = "aws.secret-access-key") secretAccessKey: String,
    ): AwsCredentialsProvider {
        log.info("Using static AWS credentials for development environment")
        val credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        return StaticCredentialsProvider.create(credentials)
    }

    /**
     * Creates an AWS credentials provider for production environments.
     * This uses the default AWS credentials provider chain, which looks for credentials in the following order:
     * 1. Java system properties: aws.accessKeyId and aws.secretAccessKey
     * 2. Environment variables: AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
     * 3. Web Identity Token credentials from the environment or container
     * 4. Credential profiles file at the default location (~/.aws/credentials)
     * 5. Amazon ECS container credentials
     * 6. Amazon EC2 Instance profile credentials
     */
    @Singleton
    @Requires(env = ["production"])
    fun prodCredentialsProvider(): AwsCredentialsProvider {
        log.info("Using default AWS credentials provider chain for production environment")
        return DefaultCredentialsProvider.create()
    }
}
