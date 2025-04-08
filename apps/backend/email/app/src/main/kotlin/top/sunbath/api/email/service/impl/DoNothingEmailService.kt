package top.sunbath.api.email.service.impl

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.email.service.EmailService

/**
 * Email service implementation that does nothing.
 */
@Singleton
@Requires(env = ["dev"])
class DoNothingEmailService : EmailService {
    private val logger = LoggerFactory.getLogger(DoNothingEmailService::class.java)

    override fun send(
        from: String,
        to: String,
        subject: String,
        html: String,
    ) {
        logger.info("Sending email to [$to] with subject [$subject] and html [$html]")
    }
}
