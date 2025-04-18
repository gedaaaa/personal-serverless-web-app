package top.sunbath.shared.dynamodb

import io.micronaut.context.condition.Condition
import io.micronaut.context.condition.ConditionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * @see <a href="https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/ec2-iam-roles.html">AWS Default credentials provider chain</a>
 */
class CIAwsCredentialsProviderChainCondition : Condition {
    private val log: Logger = LoggerFactory.getLogger(CIAwsCredentialsProviderChainCondition::class.java)

    override fun matches(context: ConditionContext<*>?): Boolean {
        if (System.getenv("CI") == null) {
            log.info("CI environment variable not present - Condition fulfilled")
            return true
        }
        if (System.getProperty("aws.accessKeyId") != null && System.getProperty("aws.secretAccessKey") != null) {
            log.info("system properties aws.accessKeyId and aws.secretAccessKey present - Condition fulfilled")
            return true
        }
        if (System.getenv("AWS_ACCESS_KEY_ID") != null && System.getenv("AWS_SECRET_ACCESS_KEY") != null) {
            log.info("environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY present - Condition fulfilled")
            return true
        }
        if (System.getenv("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI") != null) {
            log.info("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI environment variable present - Condition fulfilled")
            return true
        }
        val result = System.getenv("HOME") != null && File(System.getenv("HOME") + "/.aws/credentials").exists()
        if (result) {
            log.info("~/.aws/credentials file exists - Condition fulfilled")
        }
        return result
    }
}
