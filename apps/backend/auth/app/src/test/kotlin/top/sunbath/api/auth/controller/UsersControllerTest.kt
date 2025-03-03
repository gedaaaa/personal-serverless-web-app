package top.sunbath.api.auth.controller

import io.micronaut.http.HttpStatus
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.UpdateUserRequest
import top.sunbath.api.auth.model.User
import top.sunbath.api.auth.repository.UserRepository
import java.util.UUID

/**
 * Unit tests for the UsersController.
 */
@ExtendWith(MockKExtension::class)
class UsersControllerTest {
    @MockK
    private lateinit var userRepository: UserRepository

    @InjectMockKs
    private lateinit var controller: UsersController

    // 生成唯一用户名的辅助函数
    private fun uniqueUsername(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().substring(0, 8)}"

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `test get all users with pagination`() {
        // Given
        val limit = 10
        val cursor = null
        val users =
            listOf(
                User("1", "user1", "user1@example.com", "hashedPassword"),
                User("2", "user2", "user2@example.com", "hashedPassword"),
            )
        val nextCursor = "next_cursor_value"

        every { userRepository.findAllWithCursor(limit, cursor) } returns Pair(users, nextCursor)

        // When
        val result = controller.index(limit, cursor)

        // Then
        assertEquals(users, result.users)
        assertEquals(nextCursor, result.nextCursor)
        assertEquals(true, result.hasMore)

        verify(exactly = 1) { userRepository.findAllWithCursor(limit, cursor) }
    }

    @Test
    fun `test get user by ID when user exists`() {
        // Given
        val userId = "user123"
        val user = User(userId, "testuser", "test@example.com", "hashedPassword")

        every { userRepository.findById(userId) } returns user

        // When
        val response = controller.show(userId)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertEquals(user, response.body())

        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `test get user by ID when user does not exist`() {
        // Given
        val userId = "nonexistent"

        every { userRepository.findById(userId) } returns null

        // When
        val response = controller.show(userId)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.status)

        verify(exactly = 1) { userRepository.findById(userId) }
    }

    @Test
    fun `test create user`() {
        // Given
        val request =
            CreateUserRequest(
                username = "newuser",
                email = "new@example.com",
                password = "Password123",
                fullName = "New User",
            )
        val userId = "new_user_id"

        every {
            userRepository.save(
                request.username,
                request.email,
                request.password,
                setOf("ROLE_USER"),
                request.fullName,
            )
        } returns userId

        // When
        val response = controller.save(request)

        // Then
        assertEquals(HttpStatus.CREATED, response.status)
        val location = response.headers.get("Location")
        assertNotNull(location)
        assertEquals("/users/$userId", location)

        verify(exactly = 1) {
            userRepository.save(
                request.username,
                request.email,
                request.password,
                setOf("ROLE_USER"),
                request.fullName,
            )
        }
    }

    @Test
    fun `test update user when user exists`() {
        // Given
        val userId = "user123"
        val request =
            UpdateUserRequest(
                email = "updated@example.com",
                password = null,
                fullName = "Updated User",
                roles = setOf("ROLE_USER", "ROLE_MANAGER"),
            )

        every {
            userRepository.update(
                userId,
                request.email,
                request.password,
                request.roles,
                request.fullName,
            )
        } returns true

        // When
        val response = controller.update(userId, request)

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.status)

        verify(exactly = 1) {
            userRepository.update(
                userId,
                request.email,
                request.password,
                request.roles,
                request.fullName,
            )
        }
    }

    @Test
    fun `test update user when user does not exist`() {
        // Given
        val userId = "nonexistent"
        val request =
            UpdateUserRequest(
                email = "updated@example.com",
                password = null,
                fullName = "Updated User",
                roles = setOf("ROLE_USER"),
            )

        every {
            userRepository.update(
                userId,
                request.email,
                request.password,
                request.roles,
                request.fullName,
            )
        } returns false

        // When
        val response = controller.update(userId, request)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.status)

        verify(exactly = 1) {
            userRepository.update(
                userId,
                request.email,
                request.password,
                request.roles,
                request.fullName,
            )
        }
    }

    @Test
    fun `test delete user`() {
        // Given
        val userId = "user123"

        every { userRepository.delete(userId) } returns Unit

        // When
        val response = controller.delete(userId)

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.status)

        verify(exactly = 1) { userRepository.delete(userId) }
    }
} 
