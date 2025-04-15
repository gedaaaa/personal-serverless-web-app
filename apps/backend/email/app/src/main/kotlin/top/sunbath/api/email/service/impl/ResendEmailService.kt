package top.sunbath.api.email.service.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.email.config.ResendApiKeyProvider
import top.sunbath.api.email.config.ResendConfiguration
import top.sunbath.api.email.repository.EmailRecordRepository
import top.sunbath.api.email.service.EmailService

/**
 * Email service implementation using Resend.
 */
@Singleton
@Requires(env = ["production", "test"])
class ResendEmailService(
    private val resendConfiguration: ResendConfiguration,
    private val resendApiKeyProvider: ResendApiKeyProvider,
    private val emailRecordRepository: EmailRecordRepository,
    private val resend: Resend,
) : EmailService {
    private val logger = LoggerFactory.getLogger(ResendEmailService::class.java)

    override fun send(
        from: String,
        to: String,
        subject: String,
        html: String,
    ): String {
        if (!resendConfiguration.enabled) {
            throw IllegalStateException("Resend service is not enabled")
        }

        val objectMapper = ObjectMapper()

        logger.info("Sending email from $from to $to with subject $subject, content: $html")

        try {
            val params =
                CreateEmailOptions
                    .builder()
                    .from(from)
                    .to(to)
                    .subject(subject)
                    .html(html)
                    .build()

            val response = resend.emails().send(params)
            return emailRecordRepository.save(
                to = to,
                from = from,
                subject = subject,
                html = html,
                vendorResponse = objectMapper.writeValueAsString(response),
            )
        } catch (e: Exception) {
            // persist the error, do not retry for now.
            return emailRecordRepository.save(
                to = to,
                from = from,
                subject = subject,
                html = html,
                vendorResponse = objectMapper.writeValueAsString(e),
            )
        }
    }
}
