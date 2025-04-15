package top.sunbath.api.memo.repository

import io.micronaut.core.annotation.NonNull
import jakarta.validation.constraints.NotBlank
import top.sunbath.api.memo.model.NotificationSchedule
import java.time.Instant

/**
 * Repository interface for Memo entity operations.
 */
interface NotificationScheduleRepository {
    /**
     * Find a notification schedule by ID.
     * @param id The notification schedule ID
     * @return The notification schedule if found
     */
    @NonNull
    fun findById(
        @NonNull @NotBlank id: String,
    ): NotificationSchedule?

    /**
     * Save a new notification schedule.
     */
    @NonNull
    fun save(
        @NonNull @NotBlank id: String,
        @NonNull @NotBlank notificationId: String,
        @NonNull reminderTime: Instant,
    ): String

    /**
     * Delete an existing notification schedule.
     */
    fun delete(
        @NonNull @NotBlank id: String,
    )
}
