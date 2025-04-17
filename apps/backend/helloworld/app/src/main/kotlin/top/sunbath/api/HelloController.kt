package top.sunbath.api
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import top.sunbath.shared.GREETING_POOL
import top.sunbath.shared.random.RandomProvider

@Controller("/v1/greeting")
@Secured(SecurityRule.IS_ANONYMOUS)
open class HelloController {
    @Get
    fun getRandomGreeting(): Map<String, String> {
        val random = RandomProvider.getRandom()
        val randomIndex = random.nextInt(GREETING_POOL.size)
        return mapOf("message" to GREETING_POOL[randomIndex])
    }
}
