package top.sunbath.api.memo

import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton
import top.sunbath.api.memo.repository.impl.DefaultMemoRepository

@Requires(env = ["test"])
@Singleton
class TestBootstrap(
    private val dynamoMemoRepository: DefaultMemoRepository,
) : ApplicationEventListener<StartupEvent> {
    override fun onApplicationEvent(event: StartupEvent) {
        if (!dynamoMemoRepository.existsTable()) {
            dynamoMemoRepository.createTable()
        }
    }
}
