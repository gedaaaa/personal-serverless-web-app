package top.sunbath.api.memo.service

import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.NotificationScheduleRepository
import top.sunbath.api.memo.service.notification.NotificationService
import top.sunbath.shared.types.CurrentUser
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
     * @param memo The memo to handle the notification schedule for.
     * @param to The user to send the notification to.
     */
    open fun handleNotificationSchedule(
        memo: Memo,
        to: CurrentUser,
    ) {
        // We will use the memo id as the schedule id
        val memoId = memo.id

        // Handle null reminderTime first
        if (memo.reminderTime == null) {
            logger.info("Reminder time is null for memo: $memoId, checking for existing schedule to delete.")
            val previousSchedule = notificationScheduleRepository.findById(memoId)
            if (previousSchedule?.notificationId != null) {
                notificationService.deleteNotification(previousSchedule.notificationId)
                notificationScheduleRepository.delete(memoId)
                logger.info("Deleted existing schedule for memo $memoId because reminderTime is null.")
            }
            return // Nothing more to do if reminder time is null
        }

        // Reminder time is not null, proceed with scheduling logic
        val reminderTime = memo.reminderTime // Already checked for null
        // Use !! because we already checked for null
        val shouldCancel = memo.isCompleted || memo.isDeleted || reminderTime!! < Instant.now()

        // Find previous schedule *once*
        val previousSchedule = notificationScheduleRepository.findById(memoId)
        val previousNotificationId = previousSchedule?.notificationId

        if (shouldCancel) {
            logger.info(
                "Canceling schedule for memo: $memoId (completed: ${memo.isCompleted}, deleted: ${memo.isDeleted}, past: ${reminderTime!! < Instant.now()})",
            )
            if (previousNotificationId != null) {
                notificationService.deleteNotification(previousNotificationId)
                logger.info("Deleted notification $previousNotificationId for memo $memoId due to cancel condition.")
            }
            notificationScheduleRepository.delete(memoId)
        } else {
            // Need to schedule or reschedule

            // Check if there's an existing notification with the same reminder time
            // If so, and the memo status hasn't changed, we can avoid unnecessary delete/recreate
            if (previousNotificationId != null && previousSchedule.reminderTime == reminderTime) {
                logger.info("Existing notification $previousNotificationId for memo $memoId has the same reminder time. Skipping update.")
                return
            }

            // Delete old notification if it exists
            if (previousNotificationId != null) {
                // Delete the old one before sending the new one
                notificationService.deleteNotification(previousNotificationId)
                notificationScheduleRepository.delete(memoId) // Also delete the repository entry
                logger.info("Deleted previous notification $previousNotificationId for memo $memoId before sending new one.")
            }

            // Send the new notification
            val newNotificationId = notificationService.publishNotification(memo, to)
            if (newNotificationId != null) {
                notificationScheduleRepository.save(id = memoId, notificationId = newNotificationId, reminderTime!!)
                logger.info("Scheduled new notification $newNotificationId for memo $memoId.")
            } else {
                logger.warn("Failed to send notification for memo $memoId, schedule not saved.")
            }
        }
    }
}
