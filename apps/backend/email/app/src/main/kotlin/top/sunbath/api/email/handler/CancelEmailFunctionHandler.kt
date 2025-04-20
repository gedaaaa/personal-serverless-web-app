package top.sunbath.api.email.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.function.aws.MicronautRequestHandler
import io.micronaut.function.executor.AbstractFunctionExecutor
import org.slf4j.LoggerFactory
import top.sunbath.api.email.repository.PreventEmailJobRepository
import top.sunbath.shared.types.SqsMessage

/**
 * This handler is used to cancel an email.
 *
 * In the Lambda environment, this handler is not managed by the Micronaut container,
 *  but instead achieves dependency injection through the MicronautRequestHandler base class creating and managing the ApplicationContext.
 */
@Introspected
open class CancelEmailFunctionHandler : MicronautRequestHandler<SQSEvent, String>() {
    private var functionExecutorDelegate: CancelEmailFunctionExecutor? = null

    override fun execute(input: SQSEvent): String {
        if (functionExecutorDelegate == null) {
            functionExecutorDelegate = CancelEmailFunctionExecutor(super.getApplicationContext())
        }
        functionExecutorDelegate?.execute(input)
        return "Executed"
    }
}

/**
 * Delegate executor class for the CancelEmailFunctionHandler.
 * Separates the framework handling (Lambda integration) from business logic execution,
 *  so when we test the executor, we can control the application context.
 *
 * The email cancellation is done by a block list logic.
 * We save the job id to the block list, and before sending an email,
 *  we block the seding if the job id is in the block list.
 */
@Introspected
open class CancelEmailFunctionExecutor(
    private val applicationContext: ApplicationContext,
) : AbstractFunctionExecutor<SQSEvent, String, ApplicationContext>() {
    protected var preventEmailJobRepository: PreventEmailJobRepository

    protected val log = LoggerFactory.getLogger(this::class.java)

    protected val objectMapper = ObjectMapper()

    init {
        preventEmailJobRepository = applicationContext.getBean(PreventEmailJobRepository::class.java)
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
