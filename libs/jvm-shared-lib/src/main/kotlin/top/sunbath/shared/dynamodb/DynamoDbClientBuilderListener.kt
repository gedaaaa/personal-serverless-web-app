package top.sunbath.shared.dynamodb

import io.micronaut.context.annotation.Requires
import io.micronaut.context.annotation.Value
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.context.event.BeanCreatedEventListener
import io.micronaut.context.exceptions.ConfigurationException
import jakarta.inject.Singleton
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder
import java.net.URI
import java.net.URISyntaxException

@Requires(property = "dynamodb-local.host")
@Requires(property = "dynamodb-local.port")
@Singleton
class DynamoDbClientBuilderListener(
    @Value("\${dynamodb-local.host}") host: String,
    @Value("\${dynamodb-local.port}") port: String,
) : BeanCreatedEventListener<DynamoDbClientBuilder> {
    private val endpoint: URI
    private val accessKeyId: String = "fakeMyKeyId"
    private val secretAccessKey: String = "fakeSecretAccessKey"

    init {
        try {
            this.endpoint = URI("http://$host:$port")
        } catch (e: URISyntaxException) {
            throw ConfigurationException("dynamodb.endpoint not a valid URI")
        }
    }

    override fun onCreated(event: BeanCreatedEvent<DynamoDbClientBuilder>): DynamoDbClientBuilder {
        val credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        return event.bean
            .endpointOverride(endpoint)
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
    }
}
