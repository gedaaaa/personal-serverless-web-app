package top.sunbath.api
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import top.sunbath.shared.GREETING_POOL
import top.sunbath.shared.random.RandomProvider

@Controller("/greeting")
open class HelloController {
    @Get
    fun getRandomGreeting(): Map<String, String> {
        val random = RandomProvider.getRandom()
        val randomIndex = random.nextInt(GREETING_POOL.size)
        return mapOf("message" to GREETING_POOL[randomIndex])
    }
}
