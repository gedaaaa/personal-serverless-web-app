package top.sunbath.api.memo.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.NotificationScheduleRepository
import top.sunbath.api.memo.service.EmailTemplate
import top.sunbath.shared.sqs.SqsConfiguration
import top.sunbath.shared.types.EmailData
import java.time.Instant

/**
 * Service for handling notification schedules.
 */
@Singleton
open class NotificationScheduleService(
    private val notificationScheduleRepository: NotificationScheduleRepository,
    private val sqsClient: SqsClient,
    private val sqsConfiguration: SqsConfiguration,
) {
    private val fromAddress = "no-reply@sunbath.top"
    private val logger = LoggerFactory.getLogger(NotificationScheduleService::class.java)

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

    /**
     * Handle a notification schedule.
     * @param shouldCancle Whether to cancel previous notification schedule
     * @param id The ID of the notification schedule
     * @param notificationId The ID of the notification
     * @param reminderTime The reminder time
     */
    open fun handleNotificationSchedule(
        memo: Memo,
        to: String,
    ) {
        // We will use the memo id as the schedule id
        val memoId = memo.id ?: throw IllegalStateException("Memo ID is null")
        val reminderTime = memo.reminderTime ?: throw IllegalStateException("Reminder time is null")
        val shouldCancle = memo.isCompleted || memo.isDeleted || reminderTime > Instant.now()

        // Fisrt, delete the message from the queue
        try {
            val previousSchedule = notificationScheduleRepository.findById(memoId)
            val notificationId = previousSchedule?.notificationId
            if (notificationId != null) {
                sqsClient.deleteMessage(
                    DeleteMessageRequest
                        .builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(notificationId)
                        .build(),
                )
                logger.info("Deleted message: $notificationId for update")
            }
        } catch (e: Exception) {
            logger.error("Error deleting message: $e")
        }

        if (shouldCancle) {
            logger.info("Canceling schedule for memo: $memoId")
            notificationScheduleRepository.delete(memoId)
        } else {
            val (subject, html) =
                EmailTemplate.generateMemoNotificationEmail(
                    memo = memo,
                )
            val emailData =
                EmailData(
                    from = fromAddress,
                    to = to,
                    subject = subject,
                    html = html,
                )

            val messageBody = objectMapper.writeValueAsString(emailData)
            val response =
                sqsClient.sendMessage(
                    SendMessageRequest
                        .builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .delaySeconds((reminderTime.epochSecond - Instant.now().epochSecond - 30).toInt())
                        .build(),
                )
            val messageId = response.messageId()
            notificationScheduleRepository.save(id = memoId, notificationId = messageId, reminderTime)
        }
    }
}
