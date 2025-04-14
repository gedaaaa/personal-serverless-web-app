package top.sunbath.api.email.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.function.aws.MicronautRequestHandler
import org.slf4j.LoggerFactory
import top.sunbath.api.email.repository.PreventEmailJobRepository
import top.sunbath.shared.types.SqsMessage

@Introspected
class CancelEmailFunctionHandler : MicronautRequestHandler<SQSEvent, String>() {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper()

    private lateinit var preventEmailJobRepository: PreventEmailJobRepository

    override fun getApplicationContext(): ApplicationContext {
        val ctx = super.getApplicationContext()
        preventEmailJobRepository = ctx.getBean(PreventEmailJobRepository::class.java)
        return ctx
    }

    override fun execute(input: SQSEvent): String {
        input.records.forEach { record ->
            try {
                @Suppress("UNCHECKED_CAST")
                val message = objectMapper.readValue(record.body, SqsMessage::class.java) as SqsMessage<String>
                val jobId = message.data

                preventEmailJobRepository.save(jobId)
                log.info("Prevent Email Job $jobId saved")
            } catch (e: Exception) {
                log.error("Failed to process message: ${record.body}", e)
                throw e // 触发SQS重试
            }
        }
        return "Executed"
    }
}
