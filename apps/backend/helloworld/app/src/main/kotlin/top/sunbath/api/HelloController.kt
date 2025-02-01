package top.sunbath.api
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/hello")
open class HelloController {
    @Get
    fun greeting() = "good day?"
}
