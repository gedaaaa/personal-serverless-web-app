package top.sunbath.shared.random

import top.sunbath.shared.utils.isRunningInAwsLambda
import java.security.SecureRandom
import kotlin.random.Random

/**
 * Random Provider
 *
 * In the AWS Lambda environment, the SnapStart functionality is enabled,
 * which means that the random seed is the same across different instances that originate from the same initialization snapshot.
 *
 * To mitigate this issue, we use SecureRandom to generate the random seed upon the first request,
 * and then use this seed to initialize the Random instance.
 *
 * This approach balances performance (by only invoking the computationally expensive SecureRandom on the first request)
 * while avoiding the random seed problem in Lambda.
 *
 * In non-Lambda environments, we simply use the default Random instance.
 *
 * For scenarios requiring a high level of randomness security, it is recommended to use java.security.SecureRandom.
 */
object RandomProvider {
    private val isRunningInAwsLambda = isRunningInAwsLambda()

    private val uniqueRandom =
        ThreadLocal.withInitial {
            val secureRandom = SecureRandom()
            val seed = secureRandom.nextLong()
            Random(seed)
        }

    fun getRandom(): Random {
        if (!isRunningInAwsLambda) {
            return Random.Default
        }
        return uniqueRandom.get()
    }
}
