package top.sunbath.api.memo.service.notification

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Requires
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.service.notification.EmailTemplate
import top.sunbath.shared.sqs.SqsConfiguration
import top.sunbath.shared.types.CurrentUser
import top.sunbath.shared.types.EmailData
import top.sunbath.shared.types.SqsMessage
import java.time.Instant
import java.util.UUID

@Singleton
@Requires(env = ["production"])
class SqsEmailNotificationService(
    private val sqsClient: SqsClient,
    private val sqsConfiguration: SqsConfiguration,
) : NotificationService {
    private val fromAddress = "no-reply@sunbath.top"

    private val logger = LoggerFactory.getLogger(SqsEmailNotificationService::class.java)

    private lateinit var emailQueueUrl: String
    private lateinit var cancelEmailQueueUrl: String
    private val objectMapper = ObjectMapper()

    @PostConstruct
    fun init() {
        val queues = sqsConfiguration.getQueues()
        logger.info("Queues: $queues")

        val emailQueueName = queues.get("email") ?: throw IllegalStateException("Email queue not found in configuration")
        logger.info("Queue name: $emailQueueName")
        val getEmailQueueUrlRequest =
            GetQueueUrlRequest
                .builder()
                .queueName(emailQueueName)
                .build()
        emailQueueUrl = sqsClient.getQueueUrl(getEmailQueueUrlRequest).queueUrl()
        logger.info("Queue URL: $emailQueueUrl")

        val cancelEmailQueueName = queues.get("cancel-email") ?: throw IllegalStateException("Email queue not found in configuration")
        val getCancelEmailQueueUrlRequest =
            GetQueueUrlRequest
                .builder()
                .queueName(cancelEmailQueueName)
                .build()
        cancelEmailQueueUrl = sqsClient.getQueueUrl(getCancelEmailQueueUrlRequest).queueUrl()
        logger.info("Queue URL: $cancelEmailQueueUrl")
    }

    override fun publishNotification(
        memo: Memo,
        to: CurrentUser,
    ): String? {
        try {
            if (memo.reminderTime == null) {
                return null
            }
            val (subject, html) =
                EmailTemplate.generateMemoNotificationEmail(
                    memo = memo,
                )
            val emailData =
                EmailData(
                    from = fromAddress,
                    to = to.email,
                    subject = subject,
                    html = html,
                )

            val messageId = UUID.randomUUID().toString()

            val message =
                SqsMessage(
                    id = messageId,
                    data = emailData,
                )
            val messageBody = objectMapper.writeValueAsString(message)

            val memoReminderTime = memo.reminderTime!!
            // 30s before the reminder time
            val delaySeconds = Math.max(memoReminderTime.epochSecond - Instant.now().epochSecond - 30, 0).toInt()
            sqsClient.sendMessage(
                SendMessageRequest
                    .builder()
                    .queueUrl(emailQueueUrl)
                    .messageBody(messageBody)
                    .delaySeconds(delaySeconds)
                    .build(),
            )
            return messageId
        } catch (e: Exception) {
            logger.error("Error sending notification", e)
            return null
        }
    }

    override fun deleteNotification(id: String) {
        try {
            logger.info("Deleting notification with id [$id]")

            val message =
                SqsMessage(
                    id = UUID.randomUUID().toString(),
                    data = id,
                )
            val messageBody = objectMapper.writeValueAsString(message)

            sqsClient.sendMessage(
                SendMessageRequest
                    .builder()
                    .queueUrl(cancelEmailQueueUrl)
                    .messageBody(messageBody)
                    .build(),
            )
        } catch (e: Exception) {
            logger.error("Error deleting notification with id [$id]", e)
        }
    }
}
