package top.sunbath.api.memo.service

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.MemoListFilter
import top.sunbath.api.memo.repository.MemoRepository
import top.sunbath.api.memo.repository.MemoSort
import top.sunbath.api.memo.repository.MemoSortKey
import top.sunbath.api.memo.repository.MemoSortOrder
import top.sunbath.shared.types.UserInfo
import java.time.Instant

/**
 * Unit tests for the MemoService.
 */
@ExtendWith(MockKExtension::class)
class MemoServiceTest {
    @MockK
    private lateinit var memoRepository: MemoRepository

    @MockK
    private lateinit var notificationScheduleService: NotificationScheduleService

    private lateinit var memoService: MemoService

    private val testUserInfo =
        UserInfo(
            id = "test-user-id",
            username = "testuser",
            email = "test@example.com",
        )

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        memoService = MemoService(memoRepository, notificationScheduleService)
    }

    @Test
    fun `test createMemo success`() {
        // Given
        val title = "Test Memo"
        val content = "Test Content"
        val reminderTime = Instant.now().plusSeconds(3600)
        val memoId = "generated-memo-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                this.title = title
                this.content = content
                this.reminderTime = reminderTime
            }

        every { memoRepository.save(any(), any(), any(), any()) } returns memoId
        every { memoRepository.findById(memoId) } returns memo
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } just runs

        // When
        val result = memoService.createMemo(testUserInfo, title, content, reminderTime)

        // Then
        assertEquals(memoId, result)

        // Verify
        verify(exactly = 1) { memoRepository.save(testUserInfo.id, title, content, reminderTime) }
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test createMemo handles notification schedule error`() {
        // Given
        val title = "Test Memo"
        val content = "Test Content"
        val reminderTime = Instant.now().plusSeconds(3600)
        val memoId = "generated-memo-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                this.title = title
                this.content = content
                this.reminderTime = reminderTime
            }

        every { memoRepository.save(any(), any(), any(), any()) } returns memoId
        every { memoRepository.findById(memoId) } returns memo
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } throws RuntimeException("Test exception")

        // When
        val result = memoService.createMemo(testUserInfo, title, content, reminderTime)

        // Then
        assertEquals(memoId, result)

        // Verify
        verify(exactly = 1) { memoRepository.save(testUserInfo.id, title, content, reminderTime) }
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test getMemoById returns correct memo`() {
        // Given
        val memoId = "test-memo-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
            }

        every { memoRepository.findById(memoId) } returns memo

        // When
        val result = memoService.getMemoById(testUserInfo, memoId)

        // Then
        assertNotNull(result)
        assertEquals(memoId, result?.id)
        assertEquals(testUserInfo.id, result?.userId)

        // Verify
        verify(exactly = 1) { memoRepository.findById(memoId) }
    }

    @Test
    fun `test getMemoById returns null for non-existent memo`() {
        // Given
        val memoId = "non-existent-memo-id"

        every { memoRepository.findById(memoId) } returns null

        // When
        val result = memoService.getMemoById(testUserInfo, memoId)

        // Then
        assertNull(result)

        // Verify
        verify(exactly = 1) { memoRepository.findById(memoId) }
    }

    @Test
    fun `test getMemoById returns null for unauthorized access`() {
        // Given
        val memoId = "test-memo-id"
        val differentUserId = "different-user-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = differentUserId // Different from testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
            }

        every { memoRepository.findById(memoId) } returns memo

        // When
        val result = memoService.getMemoById(testUserInfo, memoId)

        // Then
        assertNull(result)

        // Verify
        verify(exactly = 1) { memoRepository.findById(memoId) }
    }

    @Test
    fun `test getAllMemosWithCursor returns correct memos`() {
        // Given
        val limit = 10
        val cursor = null
        val memos =
            listOf(
                Memo().apply {
                    id = "memo-1"
                    userId = testUserInfo.id
                    title = "Memo 1"
                    content = "Content 1"
                },
                Memo().apply {
                    id = "memo-2"
                    userId = testUserInfo.id
                    title = "Memo 2"
                    content = "Content 2"
                },
            )
        val nextCursor = "memo-2"

        every {
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter = any(),
                sort = any(),
            )
        } returns Pair(memos, nextCursor)

        // When
        val result = memoService.getAllMemosWithCursor(testUserInfo, limit, cursor)

        // Then
        assertEquals(memos, result.first)
        assertEquals(nextCursor, result.second)

        // Verify
        verify(exactly = 1) {
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter =
                    match<MemoListFilter> {
                        it.userId == testUserInfo.id &&
                            !it.isCompleted &&
                            !it.isDeleted
                    },
                sort =
                    match<MemoSort> {
                        it.sortOrder == MemoSortOrder.ASC &&
                            it.sortKey == MemoSortKey.REMINDER_TIME
                    },
            )
        }
    }

    @Test
    fun `test getAllMemosWithCursor with cursor returns correct page`() {
        // Given
        val limit = 10
        val cursor = "memo-2"
        val memos =
            listOf(
                Memo().apply {
                    id = "memo-3"
                    userId = testUserInfo.id
                    title = "Memo 3"
                    content = "Content 3"
                },
                Memo().apply {
                    id = "memo-4"
                    userId = testUserInfo.id
                    title = "Memo 4"
                    content = "Content 4"
                },
            )
        val nextCursor = "memo-4"

        every {
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter = any(),
                sort = any(),
            )
        } returns Pair(memos, nextCursor)

        // When
        val result = memoService.getAllMemosWithCursor(testUserInfo, limit, cursor)

        // Then
        assertEquals(memos, result.first)
        assertEquals(nextCursor, result.second)

        // Verify
        verify(exactly = 1) {
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter = any(),
                sort = any(),
            )
        }
    }

    @Test
    fun `test getAllMemosWithCursor returns empty list when no memos found`() {
        // Given
        val limit = 10
        val cursor = null
        val emptyList = emptyList<Memo>()

        every {
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter = any(),
                sort = any(),
            )
        } returns Pair(emptyList, null)

        // When
        val result = memoService.getAllMemosWithCursor(testUserInfo, limit, cursor)

        // Then
        assertEquals(emptyList, result.first)
        assertNull(result.second)

        // Verify
        verify(exactly = 1) {
            memoRepository.findAllWithCursor(
                limit = limit,
                lastEvaluatedId = cursor,
                filter = any(),
                sort = any(),
            )
        }
    }

    @Test
    fun `test updateMemo success`() {
        // Given
        val memoId = "test-memo-id"
        val updatedTitle = "Updated Title"
        val updatedContent = "Updated Content"
        val updatedReminderTime = Instant.now().plusSeconds(3600)
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Original Title"
                content = "Original Content"
                reminderTime = Instant.now()
                isCompleted = false
                isDeleted = false
            }
        val updatedMemo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = updatedTitle
                content = updatedContent
                reminderTime = updatedReminderTime
                isCompleted = true
            }

        every { memoRepository.findById(memoId) } returnsMany listOf(memo, updatedMemo)
        every { memoRepository.update(any(), any(), any(), any(), any(), any()) } returns true
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } just runs

        // When
        val result =
            memoService.updateMemo(
                testUserInfo,
                memoId,
                updatedTitle,
                updatedContent,
                updatedReminderTime,
                true,
                false,
            )

        // Then
        assertTrue(result)

        // Verify
        verify(exactly = 2) { memoRepository.findById(memoId) }
        verify(exactly = 1) {
            memoRepository.update(
                id = memoId,
                title = updatedTitle,
                content = updatedContent,
                reminderTime = updatedReminderTime,
                isCompleted = true,
                isDeleted = false,
            )
        }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test updateMemo partial update success`() {
        // Given
        val memoId = "test-memo-id"
        val updatedTitle = "Updated Title"
        val originalContent = "Original Content"
        val originalReminderTime = Instant.now()
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Original Title"
                content = originalContent
                reminderTime = originalReminderTime
                isCompleted = false
                isDeleted = false
            }
        val updatedMemo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = updatedTitle
                content = originalContent
                reminderTime = originalReminderTime
                isCompleted = false
                isDeleted = false
            }

        every { memoRepository.findById(memoId) } returnsMany listOf(memo, updatedMemo)
        every { memoRepository.update(any(), any(), any(), any(), any(), any()) } returns true
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } just runs

        // When - only update title
        val result =
            memoService.updateMemo(
                testUserInfo,
                memoId,
                updatedTitle,
                null,
                null,
                null,
                null,
            )

        // Then
        assertTrue(result)

        // Verify
        verify(exactly = 2) { memoRepository.findById(memoId) }
        verify(exactly = 1) {
            memoRepository.update(
                id = memoId,
                title = updatedTitle,
                content = originalContent,
                reminderTime = originalReminderTime,
                isCompleted = false,
                isDeleted = false,
            )
        }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test updateMemo returns false for non-existent memo`() {
        // Given
        val memoId = "non-existent-memo-id"
        val updatedTitle = "Updated Title"

        every { memoRepository.findById(memoId) } returns null

        // When
        val result =
            memoService.updateMemo(
                testUserInfo,
                memoId,
                updatedTitle,
                null,
                null,
                null,
                null,
            )

        // Then
        assertFalse(result)

        // Verify
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 0) { memoRepository.update(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { notificationScheduleService.handleNotificationSchedule(any(), any()) }
    }

    @Test
    fun `test updateMemo returns false for unauthorized access`() {
        // Given
        val memoId = "test-memo-id"
        val differentUserId = "different-user-id"
        val updatedTitle = "Updated Title"
        val memo =
            Memo().apply {
                id = memoId
                userId = differentUserId // Different from testUserInfo.id
                title = "Original Title"
                content = "Original Content"
            }

        every { memoRepository.findById(memoId) } returns memo

        // When
        val result =
            memoService.updateMemo(
                testUserInfo,
                memoId,
                updatedTitle,
                null,
                null,
                null,
                null,
            )

        // Then
        assertFalse(result)

        // Verify
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 0) { memoRepository.update(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { notificationScheduleService.handleNotificationSchedule(any(), any()) }
    }

    @Test
    fun `test updateMemo handles notification schedule error`() {
        // Given
        val memoId = "test-memo-id"
        val updatedTitle = "Updated Title"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Original Title"
                content = "Original Content"
            }
        val updatedMemo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = updatedTitle
                content = "Original Content"
            }

        every { memoRepository.findById(memoId) } returnsMany listOf(memo, updatedMemo)
        every { memoRepository.update(any(), any(), any(), any(), any(), any()) } returns true
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } throws RuntimeException("Test exception")

        // When
        val result =
            memoService.updateMemo(
                testUserInfo,
                memoId,
                updatedTitle,
                null,
                null,
                null,
                null,
            )

        // Then
        assertTrue(result)

        // Verify
        verify(exactly = 2) { memoRepository.findById(memoId) }
        verify(exactly = 1) { memoRepository.update(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test deleteMemo success`() {
        // Given
        val memoId = "test-memo-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
                isDeleted = false
            }
        val deletedMemo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
                isDeleted = true
            }

        every { memoRepository.findById(memoId) } returnsMany listOf(memo, deletedMemo)
        every { memoRepository.update(any(), any(), any(), any(), any(), any()) } returns true
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } just runs

        // When
        val result = memoService.deleteMemo(testUserInfo, memoId)

        // Then
        assertTrue(result)

        // Verify
        verify(exactly = 2) { memoRepository.findById(memoId) }
        verify(exactly = 1) {
            memoRepository.update(
                id = memoId,
                title = memo.title,
                content = memo.content,
                reminderTime = memo.reminderTime,
                isCompleted = memo.isCompleted,
                isDeleted = true,
            )
        }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test deleteMemo returns false for non-existent memo`() {
        // Given
        val memoId = "non-existent-memo-id"

        every { memoRepository.findById(memoId) } returns null

        // When
        val result = memoService.deleteMemo(testUserInfo, memoId)

        // Then
        assertFalse(result)

        // Verify
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 0) { memoRepository.update(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { notificationScheduleService.handleNotificationSchedule(any(), any()) }
    }

    @Test
    fun `test deleteMemo returns false for unauthorized access`() {
        // Given
        val memoId = "test-memo-id"
        val differentUserId = "different-user-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = differentUserId // Different from testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
            }

        every { memoRepository.findById(memoId) } returns memo

        // When
        val result = memoService.deleteMemo(testUserInfo, memoId)

        // Then
        assertFalse(result)

        // Verify
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 0) { memoRepository.update(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 0) { notificationScheduleService.handleNotificationSchedule(any(), any()) }
    }

    @Test
    fun `test deleteMemo handles notification schedule error`() {
        // Given
        val memoId = "test-memo-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
                isDeleted = false
            }
        val deletedMemo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "Test Memo"
                content = "Test Content"
                isDeleted = true
            }

        every { memoRepository.findById(memoId) } returnsMany listOf(memo, deletedMemo)
        every { memoRepository.update(any(), any(), any(), any(), any(), any()) } returns true
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } throws RuntimeException("Test exception")

        // When
        val result = memoService.deleteMemo(testUserInfo, memoId)

        // Then
        assertTrue(result)

        // Verify
        verify(exactly = 2) { memoRepository.findById(memoId) }
        verify(exactly = 1) { memoRepository.update(any(), any(), any(), any(), any(), any()) }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test handleNotificationSchedule calls service with correct parameters`() {
        // Given
        val memoId = "new-memo-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "New Memo"
                content = "New Content"
            }

        every { memoRepository.save(any(), any(), any(), any()) } returns memoId
        every { memoRepository.findById(memoId) } returns memo
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } just runs

        // When - call the private method via a public method to test its behavior
        val result = memoService.createMemo(testUserInfo, "New Memo", "New Content", null)

        // Then
        assertEquals(memoId, result)

        // Verify
        verify(exactly = 1) { memoRepository.save(testUserInfo.id, "New Memo", "New Content", null) }
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }

    @Test
    fun `test handleNotificationSchedule catches and logs exceptions`() {
        // Given
        val memoId = "new-memo-id"
        val memo =
            Memo().apply {
                id = memoId
                userId = testUserInfo.id
                title = "New Memo"
                content = "New Content"
            }

        every { memoRepository.save(any(), any(), any(), any()) } returns memoId
        every { memoRepository.findById(memoId) } returns memo
        every { notificationScheduleService.handleNotificationSchedule(any(), any()) } throws RuntimeException("Test exception")

        // When - the exception should be caught
        val result = memoService.createMemo(testUserInfo, "New Memo", "New Content", null)

        // Then - operation should complete despite the exception
        assertEquals(memoId, result)

        // Verify
        verify(exactly = 1) { memoRepository.save(testUserInfo.id, "New Memo", "New Content", null) }
        verify(exactly = 1) { memoRepository.findById(memoId) }
        verify(exactly = 1) { notificationScheduleService.handleNotificationSchedule(any(), testUserInfo) }
    }
}
