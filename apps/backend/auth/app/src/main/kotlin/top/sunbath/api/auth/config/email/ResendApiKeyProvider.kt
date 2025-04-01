package top.sunbath.api.auth.config.email

import io.micronaut.context.env.Environment
import jakarta.inject.Singleton
import top.sunbath.api.auth.config.aws.SsmParameterProvider

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
        if (!resendConfiguration.enabled) {
            throw IllegalStateException("Resend service is not enabled")
        }

        return if (environment.activeNames.contains("dev")) {
            resendConfiguration.apiKey
                ?: throw IllegalStateException("Resend API key not configured for development environment")
        } else {
            val parameterName =
                resendConfiguration.apiKeyParameter
                    ?: throw IllegalStateException("Resend API key parameter not configured for production environment")

            ssmParameterProvider.getParameter(parameterName)
        }
    }
}
