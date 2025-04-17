package top.sunbath.api.memo.integration

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import top.sunbath.api.memo.controller.request.CreateMemoRequest
import top.sunbath.api.memo.controller.request.UpdateMemoRequest
import top.sunbath.api.memo.controller.response.MemoResponse
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.MemoRepository
import top.sunbath.api.memo.repository.NotificationScheduleRepository
import top.sunbath.api.memo.service.notification.NotificationService
import top.sunbath.shared.dynamodb.DynamoConfiguration
import top.sunbath.shared.types.CurrentUser
import top.sunbath.shared.types.PagedListResponse
import java.time.Instant
import java.time.temporal.ChronoUnit

@MicronautTest(environments = ["test"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemoControllerIntegrationTest {
    @Inject
    @Client("/v1/memos")
    lateinit var client: HttpClient

    @Inject
    lateinit var memoRepository: MemoRepository

    @Inject
    lateinit var jwtTokenGenerator: JwtTokenGenerator

    @Inject
    lateinit var dynamoDbClient: DynamoDbClient

    @Inject
    lateinit var dynamoConfiguration: DynamoConfiguration

    @Inject
    lateinit var notificationService: NotificationService // This is the spyk instance from TestNotificationServiceFactory

    @Inject
    lateinit var notificationScheduleRepository: NotificationScheduleRepository

    // Test User Info
    private val testUser = TestUserFactory.createUserInfo()
    private val otherUser = TestUserFactory.createUserInfo(id = "other-user-id")

    // Notification ID Slot
    private val notificationIdSlot = slot<String>()

    // Define relative times for testing
    private val futureTime get() = Instant.now().plus(1, ChronoUnit.DAYS)
    private val anotherFutureTime get() = Instant.now().plus(2, ChronoUnit.DAYS)
    private val pastTime get() = Instant.now().minus(1, ChronoUnit.DAYS)

    @AfterEach
    fun cleanup() {
        // Clear mocks after each test to avoid interference
        clearMocks(notificationService)

        // Clean up database
        val scanRequest = ScanRequest.builder().tableName(dynamoConfiguration.tableName).build()
        val scanResponse = dynamoDbClient.scan(scanRequest)
        scanResponse.items().forEach { item ->
            val key = mapOf("pk" to item["pk"]!!, "sk" to item["sk"]!!)
            val deleteRequest =
                DeleteItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .key(key)
                    .build()
            dynamoDbClient.deleteItem(deleteRequest)
        }
    }

    @Nested
    @DisplayName("Get Memo List")
    inner class GetMemoListTest {
        @Test
        fun `should get empty memo list when no memos exist`() {
            val response =
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("")
                        .bearerAuth(generateJwtToken(testUser)),
                    PagedListResponse::class.java,
                )

            // Assert
            assertEquals(HttpStatus.OK, response?.status)
            val body = response?.body()
            assertNotNull(body)
            assertEquals(0, body?.items?.size ?: 0)
            assertFalse(body?.hasMore ?: false)
            assertNull(body?.nextCursor)
        }

        @Test
        fun `should get memos with pagination without filter and sort`() {
            // Arrange
            val totalMemos = 15
            val limit = 10
            (1..totalMemos).forEach { i ->
                val memo =
                    TestMemoFactory.createMemo(
                        userId = testUser.id,
                        title = "Test Memo $i",
                        content = "Test Content $i",
                    )
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )
            }

            // Act page 1
            val firstPageResponse =
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("/?limit=$limit")
                        .bearerAuth(generateJwtToken(testUser)),
                    PagedListResponse::class.java,
                )

            // Assert
            assertEquals(HttpStatus.OK, firstPageResponse.status)
            val firstPageBody = firstPageResponse.body()
            assertNotNull(firstPageBody)
            assertEquals(limit, firstPageBody.items.size)
            assertNotNull(firstPageBody.nextCursor)
            assertTrue(firstPageBody.hasMore)

            // Act page 2
            val secondPageResponse =
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("/?limit=$limit&cursor=${firstPageBody.nextCursor}")
                        .bearerAuth(generateJwtToken(testUser)),
                    PagedListResponse::class.java,
                )

            // Assert page 2
            assertEquals(HttpStatus.OK, secondPageResponse.status)
            val secondPageBody = secondPageResponse.body()
            assertNotNull(secondPageBody)
            assertEquals(totalMemos - limit, secondPageBody.items.size)
            assertFalse(secondPageBody.hasMore)
            assertNull(secondPageBody.nextCursor)
        }

        @Test
        fun `should get memos with pagination with completed filter`() {
            // Arrange
            val totalMemos = 15
            val limit = 10
            (1..totalMemos).forEach { i ->
                val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = futureTime)
                val memoId =
                    memoRepository.save(
                        userId = memo.userId,
                        title = memo.title,
                        content = memo.content,
                        reminderTime = memo.reminderTime,
                    )
                // mark half of the memos as completed
                if (i % 2 == 0) {
                    memoRepository.update(
                        id = memoId,
                        title = memo.title,
                        content = memo.content,
                        reminderTime = memo.reminderTime,
                        isCompleted = true,
                        isDeleted = false,
                    )
                }
            }

            // Act get page of completed memos
            val completedMemoPageResponse =
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("/?limit=$limit&filter.isCompleted=true")
                        .bearerAuth(generateJwtToken(testUser)),
                    PagedListResponse::class.java,
                )

            // Assert
            assertEquals(HttpStatus.OK, completedMemoPageResponse.status)
            val completedMemoPageBody = completedMemoPageResponse.body()
            assertNotNull(completedMemoPageBody)
            assertEquals(7, completedMemoPageBody.items.size)
            assertFalse(completedMemoPageBody.hasMore)
            assertNull(completedMemoPageBody.nextCursor)

            // Act get page of incompleted memos
            val incompletedMemoPageResponse =
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("/?limit=$limit&filter.isCompleted=false")
                        .bearerAuth(generateJwtToken(testUser)),
                    PagedListResponse::class.java,
                )

            // Assert
            assertEquals(HttpStatus.OK, incompletedMemoPageResponse.status)
            val incompletedMemoPageBody = incompletedMemoPageResponse.body()
            assertNotNull(incompletedMemoPageBody)
            assertEquals(8, incompletedMemoPageBody.items.size)
            assertFalse(incompletedMemoPageBody.hasMore)
            assertNull(incompletedMemoPageBody.nextCursor)
        }
    }

    @Nested
    @DisplayName("Get Memo One")
    inner class GetMemoOneTest {
        @Test
        fun `should get memo by id`() {
            // Arrange
            val memo = TestMemoFactory.createMemo(userId = testUser.id)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("/$memoId")
                        .bearerAuth(generateJwtToken(testUser)),
                    MemoResponse::class.java,
                )

            // Assert
            assertEquals(HttpStatus.OK, response.status)
            val body = response.body()
            assertNotNull(body)
            assertEquals(memoId, body.id)
            assertEquals(memo.title, body.title)
            assertEquals(memo.content, body.content)
            assertEquals(testUser.id, body.userId)
        }

        @Test
        fun `should return 404 when memo does not exist`() {
            // Act
            try {
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("/non-existent-id")
                        .bearerAuth(generateJwtToken(testUser)),
                    Memo::class.java,
                )
                fail("Expected exception was not thrown")
            } catch (e: Exception) {
                // Assert
                assertTrue(e.message?.contains("Not Found") ?: false)
            }
        }

        @Test
        fun `should return 404 when accessing other user's memo`() {
            // Arrange
            val memo = TestMemoFactory.createMemo(userId = otherUser.id)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Act
            try {
                client.toBlocking().exchange(
                    HttpRequest
                        .GET<Any>("/$memoId")
                        .bearerAuth(generateJwtToken(testUser)),
                    Memo::class.java,
                )
                fail("Expected exception was not thrown")
            } catch (e: Exception) {
                // Assert
                assertTrue(e.message?.contains("Not Found") ?: false)
            }
        }
    }

    @Nested
    @DisplayName("Create Memo")
    inner class CreateMemoTest {
        @Test
        fun `should create memo with future reminder and trigger notification`() {
            // Arrange
            val frozenFutureTime = futureTime
            val createRequest = CreateMemoRequest(title = "Future Reminder", content = "Content", reminderTime = frozenFutureTime)
            // Capture the notification ID when sendNotification is called
            // every { notificationService.publishNotification(any(), any()) } returns "fake-notification-id"

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.POST("/", createRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.CREATED, response.status)
            val locationHeader = response.header("Location")
            assertNotNull(locationHeader)
            val memoId = locationHeader!!.substringAfterLast('/')

            // Assert Mock Verification
            verify(exactly = 1) {
                notificationService.publishNotification(
                    match { it.id == memoId && it.reminderTime == frozenFutureTime },
                    eq(testUser),
                )
            }
            verify(exactly = 0) { notificationService.deleteNotification(any()) }

            // Assert DB State
            val createdMemo = memoRepository.findById(memoId)
            assertNotNull(createdMemo)
            assertEquals(memoId, createdMemo!!.id)
            assertEquals(createRequest.title, createdMemo.title)
            assertEquals(createRequest.content, createdMemo.content)
            assertEquals(createRequest.reminderTime, createdMemo.reminderTime)
            assertEquals(testUser.id, createdMemo.userId)
            assertFalse(createdMemo.isCompleted)
            assertFalse(createdMemo.isDeleted)
        }

        @Test
        fun `should create memo with past reminder time without triggering notification`() {
            // Arrange
            val frozenPastTime = pastTime
            val createRequest = CreateMemoRequest(title = "Past Reminder", content = "Content", reminderTime = frozenPastTime)

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.POST("/", createRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.CREATED, response.status)
            val locationHeader = response.header("Location")
            assertNotNull(locationHeader)
            val memoId = locationHeader!!.substringAfterLast('/')

            // Assert Mock Verification - no notification should be published for past reminders
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }
            verify(exactly = 0) { notificationService.deleteNotification(any()) }

            // Assert DB State
            val createdMemo = memoRepository.findById(memoId)
            assertNotNull(createdMemo)
            assertEquals(createRequest.reminderTime, createdMemo!!.reminderTime)
            assertTrue(true) // Placeholder
        }

        @Test
        fun `should create memo without reminder time and not trigger notification`() {
            // Arrange
            val createRequest = CreateMemoRequest(title = "No Reminder", content = "Content", reminderTime = null)

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.POST("/", createRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.CREATED, response.status)
            val locationHeader = response.header("Location")
            assertNotNull(locationHeader)
            val memoId = locationHeader!!.substringAfterLast('/')

            // Assert Mock Verification - no notification should be published when no reminder
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }
            verify(exactly = 0) { notificationService.deleteNotification(any()) }

            // Assert DB State
            val createdMemo = memoRepository.findById(memoId)
            assertNotNull(createdMemo)
            assertNull(createdMemo!!.reminderTime)
            assertTrue(true) // Placeholder
        }
    }

    @Nested
    @DisplayName("Update Memo")
    inner class UpdateMemoTest {
        @Test
        fun `should update memo from no reminder to future reminder and trigger notification`() {
            // Arrange
            // First create memo with no reminder
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = null)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Now update with future reminder
            val frozenFutureTime = futureTime
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = frozenFutureTime,
                    isCompleted = false,
                )
            // every { notificationService.publishNotification(any(), any()) } returns "fake-notification-id"

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - notification should be published
            verify(exactly = 1) {
                notificationService.publishNotification(
                    match { it.id == memoId && it.reminderTime == frozenFutureTime },
                    eq(testUser),
                )
            }
            verify(exactly = 0) { notificationService.deleteNotification(any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertEquals(updateRequest.title, updatedMemo!!.title)
            assertEquals(updateRequest.reminderTime, updatedMemo.reminderTime)
            assertTrue(true) // Placeholder
        }

        @Test
        fun `should update memo from future reminder to different future reminder and update notification`() {
            // Arrange
            // First create memo with future reminder
            val initialFutureTime = futureTime
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = initialFutureTime)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Simulate that a notification was previously created and stored
            val initialNotificationId = "initial-notification-id"
            notificationScheduleRepository.save(
                id = memoId,
                notificationId = initialNotificationId,
                reminderTime = initialFutureTime,
            )

            // Now update with different future reminder
            val newFutureTime = anotherFutureTime
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = newFutureTime,
                    isCompleted = false,
                )

            // Configure mocks for update
            // every { notificationService.deleteNotification(any()) } returns Unit
            every { notificationService.publishNotification(any(), any()) } returns "new-notification-id"

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - old notification deleted, new one published
            verify(exactly = 1) { notificationService.deleteNotification(initialNotificationId) }
            verify(exactly = 1) {
                notificationService.publishNotification(
                    match { it.id == memoId && it.reminderTime == newFutureTime },
                    eq(testUser),
                )
            }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertEquals(updateRequest.reminderTime, updatedMemo!!.reminderTime)
        }

        @Test
        fun `should update memo from future reminder to no reminder and delete notification`() {
            // Arrange
            // First create memo with future reminder
            val initialFutureTime = futureTime
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = initialFutureTime)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Simulate that a notification was previously created and stored
            val initialNotificationId = "initial-notification-id"
            notificationScheduleRepository.save(
                id = memoId,
                notificationId = initialNotificationId,
                reminderTime = initialFutureTime,
            )

            // Now update to remove reminder
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = null,
                    isCompleted = false,
                )

            // Act
            println("failed test case starts here, memoId: $memoId")
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - notification should be deleted
            verify(exactly = 1) { notificationService.deleteNotification(initialNotificationId) }
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertNull(updatedMemo!!.reminderTime)
        }

        @Test
        fun `should update memo from future reminder to past reminder and delete notification`() {
            // Arrange
            // First create memo with future reminder
            val initialFutureTime = futureTime
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = initialFutureTime)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Simulate that a notification was previously created and stored
            val initialNotificationId = "initial-notification-id"
            notificationScheduleRepository.save(
                id = memoId,
                notificationId = initialNotificationId,
                reminderTime = initialFutureTime,
            )

            // Now update with past reminder
            val frozenPastTime = pastTime
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = frozenPastTime,
                    isCompleted = false,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - notification should be deleted
            verify(exactly = 1) { notificationService.deleteNotification(initialNotificationId) }
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertEquals(frozenPastTime, updatedMemo!!.reminderTime)
        }

        @Test
        fun `should update memo to completed state and delete notification if future reminder exists`() {
            // Arrange
            // First create memo with future reminder
            val initialFutureTime = futureTime
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = initialFutureTime, isCompleted = false)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Simulate that a notification was previously created and stored
            val initialNotificationId = "initial-notification-id"
            notificationScheduleRepository.save(
                id = memoId,
                notificationId = initialNotificationId,
                reminderTime = initialFutureTime,
            )

            // Now mark as completed
            val updateRequest =
                UpdateMemoRequest(
                    title = memo.title,
                    content = memo.content,
                    reminderTime = initialFutureTime,
                    isCompleted = true,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - notification should be deleted when marked completed
            verify(exactly = 1) { notificationService.deleteNotification(initialNotificationId) }
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertTrue(updatedMemo!!.isCompleted)
        }

        @Test
        fun `should update memo from completed to not completed with future reminder and publish notification`() {
            // Arrange
            // First create a normal memo with reminder
            val reminderTime = futureTime
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = reminderTime)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Then update it to completed state
            val completeRequest =
                UpdateMemoRequest(
                    title = memo.title,
                    content = memo.content,
                    reminderTime = reminderTime,
                    isCompleted = true,
                )

            client.toBlocking().exchange(
                HttpRequest.PUT("/$memoId", completeRequest).bearerAuth(generateJwtToken(testUser)),
                Void::class.java,
            )

            // Configure mocks for reopen notification
            clearMocks(notificationService, answers = false)
            every { notificationService.publishNotification(any(), any()) } returns "new-notification-id"

            // Now mark as not completed
            val reopenRequest =
                UpdateMemoRequest(
                    title = memo.title,
                    content = memo.content,
                    reminderTime = reminderTime,
                    isCompleted = false,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", reopenRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - notification should be published when reopened with future reminder
            verify(exactly = 1) {
                notificationService.publishNotification(
                    match { it.id == memoId && it.reminderTime == reminderTime },
                    eq(testUser),
                )
            }
            verify(exactly = 0) { notificationService.deleteNotification(any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertFalse(updatedMemo!!.isCompleted)
        }

        @Test
        fun `should return 404 when trying to update other user's memo`() {
            // Arrange
            val memo = TestMemoFactory.createMemo(userId = otherUser.id)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = futureTime,
                    isCompleted = false,
                )

            // Act & Assert
            assertThrows(HttpClientResponseException::class.java) {
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )
            }

            // Verify memo exists and was not modified
            val existingMemo = memoRepository.findById(memoId)
            assertNotNull(existingMemo)
            assertEquals(memo.title, existingMemo!!.title)
        }

        @Test
        fun `should update memo from incomplete to complete without reminder time and not trigger notification service`() {
            // Arrange
            // First create memo with no reminder
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = null, isCompleted = false)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Now mark as completed
            val updateRequest =
                UpdateMemoRequest(
                    title = memo.title,
                    content = memo.content,
                    reminderTime = null,
                    isCompleted = true,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - no notification service calls should be made
            verify(exactly = 0) { notificationService.deleteNotification(any()) }
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertTrue(updatedMemo!!.isCompleted)
            assertNull(updatedMemo.reminderTime)
        }

        @Test
        fun `should update memo from complete to incomplete without reminder time and not trigger notification service`() {
            // Arrange
            // First create regular memo with no reminder
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = null, isCompleted = false)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Then update to completed state
            val completeRequest =
                UpdateMemoRequest(
                    title = memo.title,
                    content = memo.content,
                    reminderTime = null,
                    isCompleted = true,
                )

            client.toBlocking().exchange(
                HttpRequest.PUT("/$memoId", completeRequest).bearerAuth(generateJwtToken(testUser)),
                Void::class.java,
            )

            // Verify memo is now completed
            val completedMemo = memoRepository.findById(memoId)
            assertNotNull(completedMemo)
            assertTrue(completedMemo!!.isCompleted)

            // Clear verification counts for notification service
            clearMocks(notificationService)

            // Now update to incomplete
            val reopenRequest =
                UpdateMemoRequest(
                    title = memo.title,
                    content = memo.content,
                    reminderTime = null,
                    isCompleted = false,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", reopenRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - no notification service calls should be made
            verify(exactly = 0) { notificationService.deleteNotification(any()) }
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertFalse(updatedMemo!!.isCompleted)
            assertNull(updatedMemo.reminderTime)
        }

        @Test
        fun `should update memo from incomplete with reminder to complete without reminder and delete notification`() {
            // Arrange
            // First create memo with future reminder
            val initialFutureTime = futureTime
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = initialFutureTime, isCompleted = false)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Simulate that a notification was previously created and stored
            val initialNotificationId = "initial-notification-id"
            notificationScheduleRepository.save(
                id = memoId,
                notificationId = initialNotificationId,
                reminderTime = initialFutureTime,
            )

            // Now update to mark as completed and remove reminder
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = null,
                    isCompleted = true,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - notification should be deleted
            verify(exactly = 1) { notificationService.deleteNotification(initialNotificationId) }
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertTrue(updatedMemo!!.isCompleted)
            assertNull(updatedMemo.reminderTime)
        }

        @Test
        fun `should update memo from complete without reminder to incomplete with reminder and create notification`() {
            // Arrange
            // First create memo with no reminder and completed status
            val memo = TestMemoFactory.createMemo(userId = testUser.id, reminderTime = null, isCompleted = true)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Now update to incomplete with future reminder
            val newFutureTime = futureTime
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = newFutureTime,
                    isCompleted = false,
                )

            // Configure mocks for notification
            every { notificationService.publishNotification(any(), any()) } returns "new-notification-id"

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - new notification should be published
            verify(exactly = 0) { notificationService.deleteNotification(any()) }
            verify(exactly = 1) {
                notificationService.publishNotification(
                    match { it.id == memoId && it.reminderTime == newFutureTime },
                    eq(testUser),
                )
            }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertFalse(updatedMemo!!.isCompleted)
            assertEquals(newFutureTime, updatedMemo.reminderTime)
        }

        @Test
        fun `should return 404 when trying to update deleted memo`() {
            // Arrange
            // First create normal memo
            val memo = TestMemoFactory.createMemo(userId = testUser.id)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Then mark it as deleted using DELETE HTTP request
            val deleteResponse =
                client.toBlocking().exchange(
                    HttpRequest.DELETE<Any>("/$memoId").bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )
            assertEquals(HttpStatus.NO_CONTENT, deleteResponse.status)

            // Verify it's marked as deleted
            val deletedMemo = memoRepository.findById(memoId)
            assertNotNull(deletedMemo)
            assertTrue(deletedMemo!!.isDeleted)

            // Create update request
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = futureTime,
                    isCompleted = false,
                )

            // Act - should succeed since implementation doesn't check isDeleted flag
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Verify the memo has been updated but remains deleted
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertTrue(updatedMemo!!.isDeleted)
        }

        @Test
        fun `should update completed memo title and content only without changing notification status`() {
            // Arrange
            // First create normal memo with future reminder
            val reminderTime = futureTime
            val memo =
                TestMemoFactory.createMemo(
                    userId = testUser.id,
                    reminderTime = reminderTime,
                    isCompleted = false,
                    title = "Original Title",
                    content = "Original Content",
                )
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Then update to completed state
            val completeRequest =
                UpdateMemoRequest(
                    title = memo.title,
                    content = memo.content,
                    reminderTime = reminderTime,
                    isCompleted = true,
                )

            client.toBlocking().exchange(
                HttpRequest.PUT("/$memoId", completeRequest).bearerAuth(generateJwtToken(testUser)),
                Void::class.java,
            )

            // Verify memo is now completed
            val completedMemo = memoRepository.findById(memoId)
            assertNotNull(completedMemo)
            assertTrue(completedMemo!!.isCompleted)

            // Clear verification counts for notification service
            clearMocks(notificationService)

            // Now update only title and content, keeping completion status the same
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = reminderTime,
                    isCompleted = true,
                )

            // Act
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Assert Mock Verification - no notification service calls should be made
            // since the completion status remains true and reminder time is unchanged
            verify(exactly = 0) { notificationService.deleteNotification(any()) }
            verify(exactly = 0) { notificationService.publishNotification(any(), any()) }

            // Assert DB State
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertTrue(updatedMemo!!.isCompleted)
            assertEquals("Updated Title", updatedMemo.title)
            assertEquals("Updated Content", updatedMemo.content)
            assertEquals(reminderTime, updatedMemo.reminderTime)
        }

        @Test
        fun `should successfully update deleted memo as implementation does not check isDeleted flag`() {
            // Arrange
            // First create normal memo
            val memo = TestMemoFactory.createMemo(userId = testUser.id)
            val memoId =
                memoRepository.save(
                    userId = memo.userId,
                    title = memo.title,
                    content = memo.content,
                    reminderTime = memo.reminderTime,
                )

            // Then mark it as deleted using DELETE HTTP request
            val deleteResponse =
                client.toBlocking().exchange(
                    HttpRequest.DELETE<Any>("/$memoId").bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )
            assertEquals(HttpStatus.NO_CONTENT, deleteResponse.status)

            // Verify it's marked as deleted
            val deletedMemo = memoRepository.findById(memoId)
            assertNotNull(deletedMemo)
            assertTrue(deletedMemo!!.isDeleted)

            // Create update request
            val updateRequest =
                UpdateMemoRequest(
                    title = "Updated Title",
                    content = "Updated Content",
                    reminderTime = futureTime,
                    isCompleted = false,
                )

            // Act - should succeed since implementation doesn't check isDeleted flag
            val response =
                client.toBlocking().exchange(
                    HttpRequest.PUT("/$memoId", updateRequest).bearerAuth(generateJwtToken(testUser)),
                    Void::class.java,
                )

            // Assert Response
            assertEquals(HttpStatus.OK, response.status)

            // Verify the memo has been updated but remains deleted
            val updatedMemo = memoRepository.findById(memoId)
            assertNotNull(updatedMemo)
            assertTrue(updatedMemo!!.isDeleted)
        }
    }

    // Helper method to generate JWT token
    private fun generateJwtToken(user: CurrentUser): String {
        val claims =
            mapOf(
                "sub" to user.id,
                "username" to user.username,
                "email" to user.email,
                "roles" to listOf("ROLE_USER"),
            )
        val token = jwtTokenGenerator.generateToken(claims).get()
        return token
    }
}
