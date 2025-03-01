package top.sunbath.api.auth.config

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import java.net.URI

@Factory
@Requires(env = ["test"])
class TestDynamoDbClientFactory {
    @Singleton
    @Primary
    fun dynamoDbClient(): DynamoDbClient =
        DynamoDbClient
            .builder()
            .endpointOverride(URI.create("http://localhost:8000"))
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test", "test"),
                ),
            ).build()
}
