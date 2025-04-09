package top.sunbath.api.auth.service.email

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import top.sunbath.shared.sqs.SqsConfiguration
import top.sunbath.shared.types.EmailData

/**
 * Email service implementation using Resend.
 */
@Singleton
@Requires(env = ["production"])
@Replaces(ResendEmailService::class)
class SqsEmailService(
    private val sqsConfiguration: SqsConfiguration,
    private val sqsClient: SqsClient,
) : EmailService {
    private val fromAddress = "no-reply@sunbath.top"

    private lateinit var queueUrl: String

    private val objectMapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        val queues = sqsConfiguration.getQueues()
        logger.info("Queues: $queues")
        val queueName = queues.get("email") ?: throw IllegalStateException("Email queue not found in configuration")
        logger.info("Queue name: $queueName")
        val getQueueUrlRequest =
            GetQueueUrlRequest
                .builder()
                .queueName(queueName)
                .build()
        queueUrl = sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl()
        logger.info("Queue URL: $queueUrl")
    }

    override fun sendVerificationEmail(
        to: String,
        username: String,
        verificationToken: String,
        expiresAt: java.time.Instant,
    ) {
        val (subject, html) =
            EmailTemplate.generateVerificationEmail(
                username = username,
                verificationToken = verificationToken,
                expiresAt = expiresAt,
            )

        try {
            val emailData =
                EmailData(
                    from = fromAddress,
                    to = to,
                    subject = subject,
                    html = html,
                )

            val messageBody = objectMapper.writeValueAsString(emailData)

            sqsClient.sendMessage(
                SendMessageRequest
                    .builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .build(),
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to send verification email", e)
        }
    }
}
