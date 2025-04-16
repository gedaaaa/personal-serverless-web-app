package top.sunbath.api.memo.integration

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Requires
import io.mockk.spyk
import jakarta.inject.Singleton
import top.sunbath.api.memo.service.notification.DoNothingNotificationService
import top.sunbath.api.memo.service.notification.NotificationService

/**
 * Factory for creating a mock NotificationService for testing.
 *
 * Somehow, Micronaut will create dedicated bean instance for test framework and the application being tested.
 * So we need to ensure that the same instance is used for the framework and the application, then we can verify method calls.
 *
 * Using companion object to achieve a singleton pattern.
 */
@Factory
class TestNotificationServiceFactory {
    companion object {
        private val SINGLE_INSTANCE = spyk(DoNothingNotificationService())
    }

    @Singleton
    @Primary
    @Requires(env = ["test"])
    fun mock(): NotificationService {
        println("Factory returning instance: ${SINGLE_INSTANCE.hashCode()}")
        return SINGLE_INSTANCE
    }
}
