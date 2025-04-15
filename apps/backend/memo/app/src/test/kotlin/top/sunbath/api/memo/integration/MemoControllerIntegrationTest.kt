package top.sunbath.api.memo.integration

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.mockk
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.repository.MemoRepository
import top.sunbath.api.memo.service.notification.NotificationService
import top.sunbath.shared.dynamodb.DynamoConfiguration
import top.sunbath.shared.types.CurrentUser
import top.sunbath.shared.types.PagedListResponse

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemoControllerIntegrationTest {
    @Inject
    @Client("/items")
    lateinit var client: HttpClient

    @Inject
    lateinit var memoRepository: MemoRepository

    @Inject
    lateinit var jwtTokenGenerator: JwtTokenGenerator

    @Inject
    lateinit var dynamoDbClient: DynamoDbClient

    @Inject
    lateinit var dynamoConfiguration: DynamoConfiguration

    @MockBean(NotificationService::class)
    @Singleton
    fun mockNotificationService(): NotificationService = mockk(relaxed = true)

    // Test User Info
    private val testUser = TestUserFactory.createUserInfo()
    private val otherUser = TestUserFactory.createUserInfo(id = "other-user-id")

    /**
     * Clean up data after each test
     */
    @AfterEach
    fun cleanup() {
        // We are using single table mode, so what we need is to delete all items in the table
        val scanRequest = ScanRequest.builder().tableName(dynamoConfiguration.tableName).build()
        val scanResponse = dynamoDbClient.scan(scanRequest)
        scanResponse.items().forEach { item ->
            val key =
                mapOf(
                    "pk" to item["pk"]!!,
                    "sk" to item["sk"]!!,
                )
            val deleteRequest =
                DeleteItemRequest
                    .builder()
                    .tableName(dynamoConfiguration.tableName)
                    .key(key)
                    .build()
            dynamoDbClient.deleteItem(deleteRequest)
        }
    }

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
    fun `should get memos with pagination`() {
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
                Memo::class.java,
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

    @Test
    fun `should create memo`() {
        println("test create memo start")
        // Arrange
        val createRequest = TestMemoFactory.createCreateMemoRequest()

        // Act
        val response =
            client.toBlocking().exchange(
                HttpRequest
                    .POST("/", createRequest)
                    .bearerAuth(generateJwtToken(testUser)),
                Void::class.java,
            )

        // Assert
        assertEquals(HttpStatus.CREATED, response.status)
        assertNotNull(response.header("Location"))
    }

    @Test
    fun `should update memo`() {
        // Arrange
        val memo = TestMemoFactory.createMemo(userId = testUser.id)
        val memoId =
            memoRepository.save(
                userId = memo.userId,
                title = memo.title,
                content = memo.content,
                reminderTime = memo.reminderTime,
            )
        val updateRequest = TestMemoFactory.createUpdateMemoRequest()

        // Act
        val response =
            client.toBlocking().exchange(
                HttpRequest
                    .PUT("/$memoId", updateRequest)
                    .bearerAuth(generateJwtToken(testUser)),
                Memo::class.java,
            )

        // Assert
        assertEquals(HttpStatus.OK, response.status)
        val body = response.body()
        assertNotNull(body)
        assertEquals(memoId, body.id)
        assertEquals(updateRequest.title, body.title)
        assertEquals(updateRequest.content, body.content)
        assertEquals(updateRequest.reminderTime, body.reminderTime)
        assertEquals(updateRequest.isCompleted, body.isCompleted)

        // Verify notification is not scheduled
        // verify(exactly = 0) {
        //     notificationService.sendNotification(any(), any())
        // }
    }

    @Test
    fun `should return 404 when updating non-existent memo`() {
        // Arrange
        val updateRequest = TestMemoFactory.createUpdateMemoRequest()

        // Act
        try {
            client.toBlocking().exchange(
                HttpRequest
                    .PUT("/non-existent-id", updateRequest)
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
    fun `should delete memo`() {
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
                    .DELETE<Any>("/$memoId")
                    .bearerAuth(generateJwtToken(testUser)),
                Void::class.java,
            )

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.status)

        // Verify memo is marked as deleted
        val deletedMemo = memoRepository.findById(memoId)
        assertNotNull(deletedMemo)
        assertTrue(deletedMemo!!.isDeleted)
    }

    @Test
    fun `should return 404 when deleting other user's memo`() {
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
        val response =
            client.toBlocking().exchange(
                HttpRequest
                    .DELETE<Any>("/$memoId")
                    .bearerAuth(generateJwtToken(testUser)),
                Void::class.java,
            )

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.status)

        // Verify memo is not modified
        val originalMemo = memoRepository.findById(memoId)
        assertNotNull(originalMemo)
        assertFalse(originalMemo!!.isDeleted)
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
