package top.sunbath.api.email.config

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable

/**
 * Configuration properties for Resend email service.
 */
@ConfigurationProperties("resend")
open class ResendConfiguration {
    @NonNull
    var enabled: Boolean = false

    @Nullable
    var apiKey: String? = null

    @Nullable
    var apiKeyParameter: String? = null

    @NonNull
    var fromEmail: String = "noreply@sunbath.top"

    @NonNull
    var fromName: String = "Sunbath Auth"
}
