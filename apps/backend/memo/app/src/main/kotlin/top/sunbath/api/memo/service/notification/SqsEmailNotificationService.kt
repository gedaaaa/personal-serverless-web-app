package top.sunbath.api.memo.service.notification

import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.annotation.Requires
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.service.notification.EmailTemplate
import top.sunbath.shared.sqs.SqsConfiguration
import top.sunbath.shared.types.EmailData
import top.sunbath.shared.types.UserInfo
import java.time.Instant

@Singleton
@Requires(env = ["production"])
class SqsEmailNotificationService(
    private val sqsClient: SqsClient,
    private val sqsConfiguration: SqsConfiguration,
) : NotificationService {
    private val fromAddress = "no-reply@sunbath.top"

    private val logger = LoggerFactory.getLogger(SqsEmailNotificationService::class.java)

    private lateinit var queueUrl: String

    private val objectMapper = ObjectMapper()

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

    override fun sendNotification(
        memo: Memo,
        to: UserInfo,
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

            val messageBody = objectMapper.writeValueAsString(emailData)
            val memoReminderTime = memo.reminderTime!!
            val response =
                sqsClient.sendMessage(
                    SendMessageRequest
                        .builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .delaySeconds((memoReminderTime.epochSecond - Instant.now().epochSecond - 30).toInt())
                        .build(),
                )
            val messageId = response.messageId()
            return messageId
        } catch (e: Exception) {
            logger.error("Error sending notification", e)
            return null
        }
    }

    override fun deleteNotification(id: String) {
        try {
            logger.info("Deleting notification with id [$id]")
            sqsClient.deleteMessage(
                DeleteMessageRequest
                    .builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(id)
                    .build(),
            )
        } catch (e: Exception) {
            logger.error("Error deleting notification with id [$id]", e)
        }
    }
}
