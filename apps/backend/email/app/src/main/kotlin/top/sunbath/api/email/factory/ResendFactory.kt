package top.sunbath.api.email.factory

import com.resend.Resend
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import top.sunbath.api.email.config.ResendApiKeyProvider

/**
 * Factory for creating Resend client.
 */
@Factory
class ResendFactory(
    private val resendApiKeyProvider: ResendApiKeyProvider,
) {
    /**
     * Creates and configures a Resend client.
     *
     * @return Configured Resend client
     */
    @Singleton
    fun resend(): Resend = Resend(resendApiKeyProvider.getApiKey())
}
