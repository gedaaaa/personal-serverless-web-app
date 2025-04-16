package top.sunbath.shared.security.currentUser

import io.micronaut.context.annotation.Requires
import io.micronaut.core.bind.ArgumentBinder
import io.micronaut.core.convert.ArgumentConversionContext
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.bind.binders.TypedRequestArgumentBinder
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Singleton
import top.sunbath.shared.types.CurrentUser
import java.util.Optional

@Singleton
@Requires(property = "my-property.current-user.enabled", value = "true")
class CurrentUserBinder(
    private val securityService: SecurityService,
) : TypedRequestArgumentBinder<CurrentUser> {
    override fun argumentType(): Argument<CurrentUser> = Argument.of(CurrentUser::class.java)

    override fun bind(
        context: ArgumentConversionContext<CurrentUser>,
        source: HttpRequest<*>,
    ): ArgumentBinder.BindingResult<CurrentUser> {
        val authentication = securityService.authentication.orElse(null) ?: throw IllegalStateException("Authentication is not available")

        val userId = authentication.name
        val email = authentication.attributes["email"] as String? ?: ""
        val username = authentication.attributes["username"] as String? ?: ""

        val userInfo = CurrentUser(id = userId, username = username, email = email)

        return ArgumentBinder.BindingResult { Optional.of(userInfo) }
    }
}
