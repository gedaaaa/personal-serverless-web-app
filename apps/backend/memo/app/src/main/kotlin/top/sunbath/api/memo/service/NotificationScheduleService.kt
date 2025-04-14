package top.sunbath.api.memo.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.NotificationScheduleRepository
import top.sunbath.api.memo.service.notification.NotificationService
import top.sunbath.shared.types.UserInfo
import java.time.Instant

/**
 * Service for handling notification schedules.
 */
@Singleton
open class NotificationScheduleService(
    private val notificationScheduleRepository: NotificationScheduleRepository,
    private val notificationService: NotificationService,
) {
    private val fromAddress = "no-reply@sunbath.top"
    private val logger = LoggerFactory.getLogger(NotificationScheduleService::class.java)

    /**
     * Handle a notification schedule.
     * @param shouldCancle Whether to cancel previous notification schedule
     * @param id The ID of the notification schedule
     * @param notificationId The ID of the notification
     * @param reminderTime The reminder time
     */
    open fun handleNotificationSchedule(
        memo: Memo,
        to: UserInfo,
    ) {
        // We will use the memo id as the schedule id
        val memoId = memo.id ?: throw IllegalStateException("Memo ID is null")
        val reminderTime = memo.reminderTime ?: throw IllegalStateException("Reminder time is null")
        val shouldCancle = memo.isCompleted || memo.isDeleted || reminderTime < Instant.now()

        // First, delete the message from the queue
        val previousSchedule = notificationScheduleRepository.findById(memoId)
        val previousNotificationId = previousSchedule?.notificationId
        if (previousNotificationId != null) {
            notificationService.deleteNotification(previousNotificationId)
            logger.info("Deleted message: $previousNotificationId for update")
        }

        if (shouldCancle) {
            logger.info("Canceling schedule for memo: $memoId")
            notificationScheduleRepository.delete(memoId)
        } else {
            val notificationId = notificationService.sendNotification(memo, to)
            if (notificationId != null) {
                notificationScheduleRepository.save(id = memoId, notificationId = notificationId, reminderTime)
            }
        }
    }
}
