package top.sunbath.shared.sqs

import io.micronaut.context.annotation.ConfigurationProperties
import kotlin.collections.Map

@ConfigurationProperties("aws.sqs")
class SqsConfiguration {
    private lateinit var queues: Map<String, String>

    fun getQueues(): Map<String, String> = this.queues

    fun setQueues(queues: Map<String, String>) {
        this.queues = queues
    }
}
