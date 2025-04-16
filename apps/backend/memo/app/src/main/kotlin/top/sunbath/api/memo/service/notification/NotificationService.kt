package top.sunbath.api.memo.service.notification

import top.sunbath.api.memo.model.Memo
import top.sunbath.shared.types.CurrentUser

interface NotificationService {
    /**
     * Publish a notification that is about to happen to a user.
     *
     * @param memo The memo to publish the notification for.
     * @param to The user to publish the notification to.
     * @return The id of the notification.
     */
    fun publishNotification(
        memo: Memo,
        to: CurrentUser,
    ): String?

    /**
     * Delete a notification.
     *
     * @param id The id of the notification to delete.
     */
    fun deleteNotification(id: String)
}
