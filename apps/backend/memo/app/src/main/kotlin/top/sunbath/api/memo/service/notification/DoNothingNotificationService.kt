package top.sunbath.api.memo.service.notification

import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.memo.model.Memo
import top.sunbath.shared.types.CurrentUser

/**
 * A notification service that does nothing.
 *
 * This is used in development environments where we don't want to send notifications.
 */
@Singleton
@Requires(env = ["dev"])
open class DoNothingNotificationService : NotificationService {
    private val logger = LoggerFactory.getLogger(DoNothingNotificationService::class.java)

    override fun publishNotification(
        memo: Memo,
        to: CurrentUser,
    ): String? {
        logger.info("Publishing notification to [${to.email}] for memo [${memo.id}]")
        return "fake-notification-id"
    }

    override fun deleteNotification(id: String) {
        logger.info("Deleting notification with id [$id]")
    }
}
