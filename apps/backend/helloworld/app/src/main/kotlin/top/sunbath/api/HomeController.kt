package top.sunbath.api
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
open class HomeController {
    @Get
    fun index() = mapOf("message" to "Hello World!")
}
