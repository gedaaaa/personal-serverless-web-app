package top.sunbath.api.auth

import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton
import top.sunbath.api.auth.repository.impl.DefaultUserRepository

@Requires(env = ["test"])
@Singleton
class TestBootstrap(
    private val dynamoUserRepository: DefaultUserRepository,
) : ApplicationEventListener<StartupEvent> {
    override fun onApplicationEvent(event: StartupEvent) {
        if (!dynamoUserRepository.existsTable()) {
            dynamoUserRepository.createTable()
        }
    }
}
