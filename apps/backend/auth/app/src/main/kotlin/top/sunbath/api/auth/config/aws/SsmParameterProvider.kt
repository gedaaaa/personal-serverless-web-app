package top.sunbath.api.auth.config.aws

import io.micronaut.cache.annotation.Cacheable
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException

/**
 * Provider for AWS SSM parameters with caching support.
 */
@Singleton
open class SsmParameterProvider(
    private val ssmClient: SsmClient,
) {
    @Value("\${aws.region}")
    private lateinit var region: String

    /**
     * Get a parameter value from SSM with caching.
     * Cache duration is controlled by Micronaut's cache configuration.
     *
     * @param parameterName The name of the parameter to get
     * @return The parameter value
     * @throws IllegalStateException if the parameter is not found
     */
    @Cacheable("ssm-parameters")
    open fun getParameter(parameterName: String): String {
        try {
            val request =
                GetParameterRequest
                    .builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build()

            return ssmClient.getParameter(request).parameter().value()
        } catch (e: ParameterNotFoundException) {
            throw IllegalStateException("Parameter $parameterName not found in SSM", e)
        }
    }

    /**
     * Get a parameter value from SSM with caching, returning null if not found.
     *
     * @param parameterName The name of the parameter to get
     * @return The parameter value, or null if not found
     */
    @Cacheable("ssm-parameters")
    open fun getParameterOrNull(parameterName: String): String? =
        try {
            val request =
                GetParameterRequest
                    .builder()
                    .name(parameterName)
                    .withDecryption(true)
                    .build()

            ssmClient.getParameter(request).parameter().value()
        } catch (e: ParameterNotFoundException) {
            null
        }
}
