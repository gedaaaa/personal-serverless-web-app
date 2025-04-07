package top.sunbath.api.auth.devDbUtil.seeder

import at.favre.lib.crypto.bcrypt.BCrypt
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.auth.devDbUtil.DevBootstrap
import top.sunbath.api.auth.repository.UserRepository

/**
 * User data initializer, only runs in development environment
 */
@Singleton
@Requires(env = [Environment.DEVELOPMENT])
@Requires(beans = [DevBootstrap::class])
class UserSeeder(
    private val userRepository: UserRepository,
) : ApplicationEventListener<StartupEvent> {
    private val logger = LoggerFactory.getLogger(UserSeeder::class.java)

    companion object {
        private const val BCRYPT_COST = 12
    }

    override fun onApplicationEvent(event: StartupEvent) {
        logger.info("Starting user data initialization...")

        // Check if admin user exists
        val adminUser = userRepository.findByUsername("admin")
        if (adminUser == null) {
            // Create admin user
            val adminPassword = hashPassword("Admin123") // Should use stronger password in production
            val adminId =
                userRepository.save(
                    username = "admin",
                    email = "admin@example.com",
                    password = adminPassword,
                    roles = setOf("ROLE_USER", "ROLE_ADMIN"),
                    fullName = "System Administrator",
                    emailVerified = true,
                    emailVerificationToken = null,
                    emailVerificationTokenExpiresAt = null,
                    lastVerificationEmailSentAt = null,
                )
            logger.info("Admin user created successfully, ID: $adminId")
        } else {
            logger.info("Admin user already exists, skipping creation")
        }

        // Create regular users
        val regularUsernames = listOf("user1", "user2", "user3", "user4", "user5")

        regularUsernames.forEachIndexed { index, username ->
            val user = userRepository.findByUsername(username)
            if (user == null) {
                val password = hashPassword("User123${index + 1}") // Should use stronger password in production
                val userId =
                    userRepository.save(
                        username = username,
                        email = "$username@example.com",
                        password = password,
                        roles = setOf("ROLE_USER"),
                        fullName = "Test User ${index + 1}",
                        emailVerified = true,
                        emailVerificationToken = null,
                        emailVerificationTokenExpiresAt = null,
                        lastVerificationEmailSentAt = null,
                    )
                logger.info("Regular user $username created successfully, ID: $userId")
            } else {
                logger.info("User $username already exists, skipping creation")
            }
        }

        logger.info("User data initialization completed")
    }

    /**
     * Hash password using BCrypt
     * @param password Plain text password
     * @return Hashed password
     */
    private fun hashPassword(password: String): String = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
}
