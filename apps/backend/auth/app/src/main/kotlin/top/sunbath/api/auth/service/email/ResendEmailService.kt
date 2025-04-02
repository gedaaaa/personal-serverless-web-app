package top.sunbath.api.auth.service.email

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import top.sunbath.api.auth.config.email.ResendApiKeyProvider
import top.sunbath.api.auth.config.email.ResendConfiguration

/**
 * Email service implementation using Resend.
 */
@Singleton
@Requires(env = ["production"])
class ResendEmailService(
    private val resendConfiguration: ResendConfiguration,
    private val resendApiKeyProvider: ResendApiKeyProvider,
) : EmailService {
    override fun sendVerificationEmail(
        to: String,
        username: String,
        verificationToken: String,
        expiresAt: java.time.Instant,
    ) {
        if (!resendConfiguration.enabled) {
            throw IllegalStateException("Resend service is not enabled")
        }

        val (subject, html) =
            EmailTemplate.generateVerificationEmail(
                username = username,
                verificationToken = verificationToken,
                expiresAt = expiresAt,
            )

        val resend = Resend(resendApiKeyProvider.getApiKey())

        try {
            val params =
                CreateEmailOptions
                    .builder()
                    .from(resendConfiguration.fromEmail)
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build()

            resend.emails().send(params)
        } catch (e: Exception) {
            throw RuntimeException("Failed to send verification email", e)
        }
    }
}
