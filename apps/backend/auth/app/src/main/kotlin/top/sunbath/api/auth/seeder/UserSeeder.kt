package top.sunbath.api.auth.seeder

import at.favre.lib.crypto.bcrypt.BCrypt
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.auth.repository.UserRepository

/**
 * 用户数据初始化器，仅在开发环境中运行
 */
@Singleton
@Requires(env = [Environment.DEVELOPMENT])
class UserSeeder(
    private val userRepository: UserRepository,
) : ApplicationEventListener<StartupEvent> {
    private val logger = LoggerFactory.getLogger(UserSeeder::class.java)

    companion object {
        private const val BCRYPT_COST = 12
    }

    override fun onApplicationEvent(event: StartupEvent) {
        logger.info("开始初始化用户数据...")

        // 检查是否已存在管理员用户
        val adminUser = userRepository.findByUsername("admin")
        if (adminUser == null) {
            // 创建管理员用户
            val adminPassword = hashPassword("Admin123") // 实际环境中应使用更强的密码
            val adminId =
                userRepository.save(
                    username = "admin",
                    email = "admin@example.com",
                    password = adminPassword,
                    roles = setOf("ROLE_USER", "ROLE_ADMIN"),
                    fullName = "系统管理员",
                    emailVerified = true,
                    emailVerificationToken = null,
                    emailVerificationTokenExpiresAt = null,
                    lastVerificationEmailSentAt = null,
                )
            logger.info("创建管理员用户成功，ID: $adminId")
        } else {
            logger.info("管理员用户已存在，跳过创建")
        }

        // 创建普通用户
        val regularUsernames = listOf("user1", "user2", "user3", "user4", "user5")

        regularUsernames.forEachIndexed { index, username ->
            val user = userRepository.findByUsername(username)
            if (user == null) {
                val password = hashPassword("User123${index + 1}") // 实际环境中应使用更强的密码
                val userId =
                    userRepository.save(
                        username = username,
                        email = "$username@example.com",
                        password = password,
                        roles = setOf("ROLE_USER"),
                        fullName = "测试用户 ${index + 1}",
                        emailVerified = true,
                        emailVerificationToken = null,
                        emailVerificationTokenExpiresAt = null,
                        lastVerificationEmailSentAt = null,
                    )
                logger.info("创建普通用户 $username 成功，ID: $userId")
            } else {
                logger.info("用户 $username 已存在，跳过创建")
            }
        }

        logger.info("用户数据初始化完成")
    }

    /**
     * 使用 BCrypt 对密码进行加密
     * @param password 明文密码
     * @return 加密后的密码
     */
    private fun hashPassword(password: String): String = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
}
