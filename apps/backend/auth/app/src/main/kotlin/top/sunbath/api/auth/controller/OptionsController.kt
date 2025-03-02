package top.sunbath.api.auth.controller

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Options
import io.micronaut.http.annotation.PathVariable

@Controller("/")
class OptionsController {
    /**
     * TODO: Find a better solution.
     * Workaround for OPTIONS preflight not return properly.
     * AWS http api with CORS configured will ignore OPTIONS response **headers** from origin
     * but will follow the status code. So, if we enable CORS on server, browser will not receive proper allow method headers.
     * And if we disable CORS, because io.micronaut.function.aws.proxy.payload2.APIGatewayV2HTTPEventFunction
     * is not handling OPTIONS, browser will get 405.
     * So we disable CORS, and add OPTIONS controller for all possible path and always return 200.
     * Let API gateway handle the real CORS response.
     */
    @Options("{/path:.*}")
    fun handleOptions(
        @Suppress("unused")
        @Nullable
        @PathVariable path: String?,
    ) {
        // let the cors filter do its job
    }
}
