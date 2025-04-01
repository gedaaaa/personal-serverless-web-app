package top.sunbath.api.auth.config.aws

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmClient

/**
 * Configuration for AWS SSM client.
 */
@Factory
class SsmConfiguration {
    @Value("\${aws.region}")
    private lateinit var region: String

    /**
     * Creates a singleton SSM client.
     * @return The SSM client
     */
    @Singleton
    fun ssmClient(): SsmClient =
        SsmClient
            .builder()
            .region(Region.of(region))
            .build()
}
