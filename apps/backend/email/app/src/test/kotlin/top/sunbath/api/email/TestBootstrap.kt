package top.sunbath.api.email

import io.micronaut.context.annotation.Requires
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import jakarta.inject.Singleton
import top.sunbath.api.email.repository.impl.DefaultEmailRecordRepository

@Requires(env = ["test"])
@Singleton
class TestBootstrap(
    private val dynamoEmailRecordRepository: DefaultEmailRecordRepository,
) : ApplicationEventListener<StartupEvent> {
    override fun onApplicationEvent(event: StartupEvent) {
        if (!dynamoEmailRecordRepository.existsTable()) {
            dynamoEmailRecordRepository.createTable()
        }
    }
}
