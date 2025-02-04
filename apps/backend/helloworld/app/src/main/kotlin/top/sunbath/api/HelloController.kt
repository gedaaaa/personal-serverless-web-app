package top.sunbath.api
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import top.sunbath.shared.GREETING_POOL
import kotlin.random.Random

@Controller("/greeting")
open class HelloController {
    @Get
    fun getRandomGreeting(): Map<String, String> {
        val randomIndex = Random.nextInt(GREETING_POOL.size)
        return mapOf("message" to GREETING_POOL[randomIndex])
    }
}
