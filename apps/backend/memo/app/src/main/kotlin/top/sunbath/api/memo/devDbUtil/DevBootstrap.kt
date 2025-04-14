package top.sunbath.api.memo.devDbUtil

import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.Environment
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton
import top.sunbath.api.memo.repository.impl.DefaultMemoRepository

@Requires(property = "dynamodb-local.host")
@Requires(property = "dynamodb-local.port")
@Requires(env = [Environment.DEVELOPMENT])
@Singleton
open class DevBootstrap(
    private val dynamoMemoRepository: DefaultMemoRepository,
) : ApplicationEventListener<StartupEvent> {
    override fun onApplicationEvent(event: StartupEvent) {
        if (!dynamoMemoRepository.existsTable()) {
            dynamoMemoRepository.createTable()
        }
    }
}
