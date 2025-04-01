package top.sunbath.api.auth.service.email

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

/**
 * Email service implementation that does nothing.
 */
@Singleton
@Requires(env = ["dev"])
class DevEmailService : EmailService {
    private val logger = LoggerFactory.getLogger(DevEmailService::class.java)

    override fun sendVerificationEmail(
        to: String,
        username: String,
        verificationToken: String,
        expiresAt: java.time.Instant,
    ) {
        logger.info("Sending verification email to [$to] with token [$verificationToken]")
    }
}
