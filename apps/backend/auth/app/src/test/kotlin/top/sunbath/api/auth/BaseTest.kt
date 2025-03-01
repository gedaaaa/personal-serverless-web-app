package top.sunbath.api.auth

import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import top.sunbath.api.auth.repository.UserRepository

/**
 * Base test class for all tests that need DynamoDB access.
 * Provides cleanup functionality after each test.
 */
abstract class BaseTest : TestPropertyProvider {
    @Inject
    lateinit var userRepository: UserRepository

    override fun getProperties(): Map<String, String> =
        mapOf(
            "dynamodb.table-name" to "users-test",
        )

    @AfterEach
    fun cleanup() {
        // Clean up all users after each test
        userRepository.findAll().forEach { user ->
            user.id?.let { userRepository.delete(it) }
        }
    }
}
