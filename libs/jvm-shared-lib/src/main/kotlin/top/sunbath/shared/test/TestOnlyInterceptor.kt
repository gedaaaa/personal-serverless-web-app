package top.sunbath.shared.test

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.context.env.Environment

@InterceptorBean(TestOnly::class)
class TestOnlyInterceptor(
    private val environment: Environment,
) : MethodInterceptor<Any, Any> {
    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        if (environment.getActiveNames().contains("test")) {
            return context.proceed()
        }
        throw UnsupportedOperationException("Method not allowed in current environment")
    }
}
