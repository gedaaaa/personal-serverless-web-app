package top.sunbath.api.memo.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.model.NotificationSchedule
import top.sunbath.api.memo.repository.NotificationScheduleRepository
import top.sunbath.api.memo.service.notification.NotificationService
import top.sunbath.shared.types.UserInfo
import java.time.Instant

/**
 * Unit tests for the NotificationScheduleService.
 */
@ExtendWith(MockKExtension::class)
class NotificationScheduleServiceTest {
    @MockK
    private lateinit var notificationScheduleRepository: NotificationScheduleRepository

    @MockK
    private lateinit var notificationService: NotificationService

    private lateinit var notificationScheduleService: NotificationScheduleService

    private val testUserInfo =
        UserInfo(
            id = "test-user-id",
            username = "testuser",
            email = "test@example.com",
        )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        notificationScheduleService =
            NotificationScheduleService(
                notificationScheduleRepository,
                notificationService,
            )
    }

    @Test
    fun `test handleNotificationSchedule creates new notification for first time memo`() {
        // Given
        val memoId = "test-memo-id"
        val futureTime = Instant.now().plusSeconds(3600)
        val newNotificationId = "new-notification-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
                reminderTime = futureTime
                isCompleted = false
                isDeleted = false
            }

        every { notificationScheduleRepository.findById(memoId) } returns null
        every { notificationService.sendNotification(memo, testUserInfo) } returns newNotificationId
        every { notificationScheduleRepository.save(memoId, newNotificationId, futureTime) } returns memoId

        // When
        notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)

        // Then
        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 0) { notificationService.deleteNotification(any()) }
        verify(exactly = 1) { notificationService.sendNotification(memo, testUserInfo) }
        verify(exactly = 1) { notificationScheduleRepository.save(memoId, newNotificationId, futureTime) }
    }

    @Test
    fun `test handleNotificationSchedule updates existing notification`() {
        // Given
        val memoId = "test-memo-id"
        val oldNotificationId = "old-notification-id"
        val newNotificationId = "new-notification-id"
        val futureTime = Instant.now().plusSeconds(3600)
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Updated Memo"
                content = "Updated Content"
                reminderTime = futureTime
                isCompleted = false
                isDeleted = false
            }
        val existingSchedule =
            NotificationSchedule().apply {
                id = memoId
                notificationId = oldNotificationId
                reminderTime = Instant.now().plusSeconds(1800) // Earlier time
            }

        every { notificationScheduleRepository.findById(memoId) } returns existingSchedule
        every { notificationService.deleteNotification(oldNotificationId) } just runs
        every { notificationService.sendNotification(memo, testUserInfo) } returns newNotificationId
        every { notificationScheduleRepository.save(memoId, newNotificationId, futureTime) } returns memoId

        // When
        notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)

        // Then
        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 1) { notificationService.deleteNotification(oldNotificationId) }
        verify(exactly = 1) { notificationService.sendNotification(memo, testUserInfo) }
        verify(exactly = 1) { notificationScheduleRepository.save(memoId, newNotificationId, futureTime) }
    }

    @Test
    fun `test handleNotificationSchedule cancels notification when memo is completed`() {
        // Given
        val memoId = "test-memo-id"
        val oldNotificationId = "old-notification-id"
        val futureTime = Instant.now().plusSeconds(3600)
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Completed Memo"
                content = "Completed Content"
                reminderTime = futureTime
                isCompleted = true // Memo is completed
                isDeleted = false
            }
        val existingSchedule =
            NotificationSchedule().apply {
                id = memoId
                notificationId = oldNotificationId
                reminderTime = futureTime
            }

        every { notificationScheduleRepository.findById(memoId) } returns existingSchedule
        every { notificationService.deleteNotification(oldNotificationId) } just runs
        every { notificationScheduleRepository.delete(memoId) } just runs

        // When
        notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)

        // Then
        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 1) { notificationService.deleteNotification(oldNotificationId) }
        verify(exactly = 0) { notificationService.sendNotification(any(), any()) }
        verify(exactly = 1) { notificationScheduleRepository.delete(memoId) }
        verify(exactly = 0) { notificationScheduleRepository.save(any(), any(), any()) }
    }

    @Test
    fun `test handleNotificationSchedule cancels notification when memo is deleted`() {
        // Given
        val memoId = "test-memo-id"
        val oldNotificationId = "old-notification-id"
        val futureTime = Instant.now().plusSeconds(3600)
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Deleted Memo"
                content = "Deleted Content"
                reminderTime = futureTime
                isCompleted = false
                isDeleted = true // Memo is deleted
            }
        val existingSchedule =
            NotificationSchedule().apply {
                id = memoId
                notificationId = oldNotificationId
                reminderTime = futureTime
            }

        every { notificationScheduleRepository.findById(memoId) } returns existingSchedule
        every { notificationService.deleteNotification(oldNotificationId) } just runs
        every { notificationScheduleRepository.delete(memoId) } just runs

        // When
        notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)

        // Then
        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 1) { notificationService.deleteNotification(oldNotificationId) }
        verify(exactly = 0) { notificationService.sendNotification(any(), any()) }
        verify(exactly = 1) { notificationScheduleRepository.delete(memoId) }
        verify(exactly = 0) { notificationScheduleRepository.save(any(), any(), any()) }
    }

    @Test
    fun `test handleNotificationSchedule cancels notification when reminder time is past`() {
        // Given
        val memoId = "test-memo-id"
        val oldNotificationId = "old-notification-id"
        val pastTime = Instant.now().minusSeconds(3600) // Past time
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Past Memo"
                content = "Past Content"
                reminderTime = pastTime
                isCompleted = false
                isDeleted = false
            }
        val existingSchedule =
            NotificationSchedule().apply {
                id = memoId
                notificationId = oldNotificationId
                reminderTime = pastTime
            }

        every { notificationScheduleRepository.findById(memoId) } returns existingSchedule
        every { notificationService.deleteNotification(oldNotificationId) } just runs
        every { notificationScheduleRepository.delete(memoId) } just runs

        // When
        notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)

        // Then
        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 1) { notificationService.deleteNotification(oldNotificationId) }
        verify(exactly = 0) { notificationService.sendNotification(any(), any()) }
        verify(exactly = 1) { notificationScheduleRepository.delete(memoId) }
        verify(exactly = 0) { notificationScheduleRepository.save(any(), any(), any()) }
    }

    @Test
    fun `test handleNotificationSchedule handles exception when sending notification fails`() {
        // Given
        val memoId = "test-memo-id"
        val futureTime = Instant.now().plusSeconds(3600)
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
                reminderTime = futureTime
                isCompleted = false
                isDeleted = false
            }

        every { notificationScheduleRepository.findById(memoId) } returns null
        every { notificationService.sendNotification(memo, testUserInfo) } throws RuntimeException("Failed to send notification")

        // When & Then
        assertThrows(RuntimeException::class.java) {
            notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)
        }

        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 0) { notificationService.deleteNotification(any()) }
        verify(exactly = 1) { notificationService.sendNotification(memo, testUserInfo) }
        verify(exactly = 0) { notificationScheduleRepository.save(any(), any(), any()) }
    }

    @Test
    fun `test handleNotificationSchedule handles exception when deleting notification fails`() {
        // Given
        val memoId = "test-memo-id"
        val oldNotificationId = "old-notification-id"
        val futureTime = Instant.now().plusSeconds(3600)
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Updated Memo"
                content = "Updated Content"
                reminderTime = futureTime
                isCompleted = false
                isDeleted = false
            }
        val existingSchedule =
            NotificationSchedule().apply {
                id = memoId
                notificationId = oldNotificationId
                reminderTime = Instant.now().plusSeconds(1800) // Earlier time
            }

        every { notificationScheduleRepository.findById(memoId) } returns existingSchedule
        every { notificationService.deleteNotification(oldNotificationId) } throws RuntimeException("Failed to delete notification")

        // When & Then
        assertThrows(RuntimeException::class.java) {
            notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)
        }

        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 1) { notificationService.deleteNotification(oldNotificationId) }
        verify(exactly = 0) { notificationService.sendNotification(any(), any()) }
        verify(exactly = 0) { notificationScheduleRepository.save(any(), any(), any()) }
    }

    @Test
    fun `test handleNotificationSchedule handles exception when saving notification schedule fails`() {
        // Given
        val memoId = "test-memo-id"
        val futureTime = Instant.now().plusSeconds(3600)
        val newNotificationId = "new-notification-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
                reminderTime = futureTime
                isCompleted = false
                isDeleted = false
            }

        every { notificationScheduleRepository.findById(memoId) } returns null
        every { notificationService.sendNotification(memo, testUserInfo) } returns newNotificationId
        every { notificationScheduleRepository.save(memoId, newNotificationId, futureTime) } throws
            RuntimeException("Failed to save notification schedule")

        // When & Then
        assertThrows(RuntimeException::class.java) {
            notificationScheduleService.handleNotificationSchedule(memo, testUserInfo)
        }

        verify(exactly = 1) { notificationScheduleRepository.findById(memoId) }
        verify(exactly = 0) { notificationService.deleteNotification(any()) }
        verify(exactly = 1) { notificationService.sendNotification(memo, testUserInfo) }
        verify(exactly = 1) { notificationScheduleRepository.save(memoId, newNotificationId, futureTime) }
    }
} 
