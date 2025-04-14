package top.sunbath.shared.security.currentUser

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.core.type.MutableArgumentValue
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.utils.SecurityService
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.shared.types.UserInfo

/**
 * Method interceptor that resolves @CurrentUser annotated parameters in controller methods.
 * Retrieves the current user ID from JWT and injects it into the method parameter.
 */
@Singleton
@InterceptorBean(CurrentUser::class)
class CurrentUserMethodInterceptor(
    private val securityService: SecurityService,
) : MethodInterceptor<Any, Any> {
    private val logger = LoggerFactory.getLogger(CurrentUserMethodInterceptor::class.java)

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        // Get the parameters annotated with @CurrentUser
        val parameters =
            context.parameters
                .filter { it.value.annotationMetadata.hasAnnotation(CurrentUser::class.java) }

        if (parameters.isEmpty()) {
            // No @CurrentUser parameters, just proceed with the method invocation
            return context.proceed()
        }

        // Get current authentication
        val authentication: Authentication? = securityService.authentication.orElse(null)

        logger.info("authentication: $authentication")

        if (authentication == null) {
            logger.warn("No authenticated user found when resolving @CurrentUser")
            throw SecurityException("No authenticated user found")
        }

        // Get user ID from JWT claims (sub field)
        val userId = authentication.attributes["sub"] as String
        val username = authentication.attributes["username"] as String
        val email = authentication.attributes["email"] as String

        val userInfo =
            UserInfo(
                id = userId,
                username = username,
                email = email,
            )

        // Set user ID for all @CurrentUser parameters
        parameters.forEach { (name, _) ->
            val param = context.parameters[name]
            if (param is MutableArgumentValue<*> && param.type == UserInfo::class.java) {
                @Suppress("UNCHECKED_CAST")
                (param as MutableArgumentValue<UserInfo>).setValue(userInfo)
            }
        }

        // Proceed with the method invocation
        return context.proceed()
    }
}
