package top.sunbath.api.auth.dynamodbUtil

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Property
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.regions.providers.AwsRegionProvider
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain

/**
 * Factory for creating AWS region providers.
 */
@Factory
class AwsRegionProviderFactory {
    private val log = LoggerFactory.getLogger(AwsRegionProviderFactory::class.java)

    /**
     * Creates an AWS region provider using the region from configuration.
     */
    @Singleton
    @Requires(property = "aws.region")
    fun configRegionProvider(
        @Property(name = "aws.region") regionName: String,
    ): AwsRegionProvider {
        log.info("Using AWS region from configuration: $regionName")
        return AwsRegionProvider { Region.of(regionName) }
    }

    /**
     * Creates a default AWS region provider when no region is specified in configuration.
     * This uses the default AWS region provider chain.
     */
    @Singleton
    @Requires(missingProperty = "aws.region")
    fun defaultRegionProvider(): AwsRegionProvider {
        log.info("Using default AWS region provider chain")
        return DefaultAwsRegionProviderChain.builder().build()
    }
}
