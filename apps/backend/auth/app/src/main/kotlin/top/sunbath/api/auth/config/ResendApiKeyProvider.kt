package top.sunbath.api.auth.config

import io.micronaut.context.env.Environment
import jakarta.inject.Singleton
import top.sunbath.shared.aws.SsmParameterProvider

/**
 * Provider for Resend API key, handling different environments.
 */
@Singleton
class ResendApiKeyProvider(
    private val resendConfiguration: ResendConfiguration,
    private val environment: Environment,
    private val ssmParameterProvider: SsmParameterProvider,
) {
    /**
     * Get the Resend API key based on the current environment.
     * For development, uses the configured API key.
     * For production, fetches the API key from AWS SSM Parameter Store.
     *
     * @return The Resend API key
     * @throws IllegalStateException if the API key is not properly configured
     */
    fun getApiKey(): String {
        val parameterName =
            resendConfiguration.apiKeyParameter
                ?: throw IllegalStateException("Resend API key parameter not configured for production environment")

        return ssmParameterProvider.getParameter(parameterName)
    }
}
