package top.sunbath.api
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import top.sunbath.shared.GREETING_POOL
import kotlin.random.Random

@Controller("/greeting")
open class HelloController {
    @Get
    fun getRandomGreeting(): String {
        val randomIndex = Random.nextInt(GREETING_POOL.size)
        return GREETING_POOL[randomIndex]
    }

}
