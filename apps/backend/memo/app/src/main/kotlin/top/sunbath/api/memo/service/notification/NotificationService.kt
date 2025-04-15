package top.sunbath.api.memo.service.notification

import top.sunbath.api.memo.model.Memo
import top.sunbath.shared.types.CurrentUser

interface NotificationService {
    /**
     * Send a notification to a user.
     *
     * @param memo The memo to send the notification for.
     * @param to The user to send the notification to.
     * @return The id of the notification.
     */
    fun sendNotification(
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
