package top.sunbath.api.email.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.function.aws.MicronautRequestHandler
import io.micronaut.function.executor.AbstractFunctionExecutor
import org.slf4j.LoggerFactory
import top.sunbath.api.email.repository.PreventEmailJobRepository
import top.sunbath.api.email.service.EmailService
import top.sunbath.shared.types.EmailData
import top.sunbath.shared.types.SqsMessage

/**
 * This handler is used to send emails.
 *
 * In the Lambda environment, this handler is not managed by the Micronaut container,
 *  but instead achieves dependency injection through the MicronautRequestHandler base class creating and managing the ApplicationContext.
 *
 */
@Introspected
open class EmailFunctionHandler : MicronautRequestHandler<SQSEvent, String>() {
    private var functionEexecutorDelegate: EmailFunctionExecutor? = null

    override fun execute(input: SQSEvent): String {
        if (functionEexecutorDelegate == null) {
            functionEexecutorDelegate = EmailFunctionExecutor(super.getApplicationContext())
        }
        functionEexecutorDelegate?.execute(input)
        return "Executed"
    }
}

/**
 * Delegate executor class for the EmailFunctionHandler.
 * Separates the framework handling (Lambda integration) from business logic execution,
 *  so when we test the executor, we can control the application context.
 */
@Introspected
open class EmailFunctionExecutor(
    private val applicationContext: ApplicationContext,
) : AbstractFunctionExecutor<SQSEvent, List<String>, ApplicationContext>() {
    protected var emailService: EmailService
    protected var preventEmailJobRepository: PreventEmailJobRepository

    protected val log = LoggerFactory.getLogger(this::class.java)
    protected val objectMapper = ObjectMapper()

    init {
        emailService = applicationContext.getBean(EmailService::class.java)
        preventEmailJobRepository = applicationContext.getBean(PreventEmailJobRepository::class.java)
    }

    override fun execute(input: SQSEvent): List<String> {
        val result = mutableListOf<String>()
        input.records.forEach { record ->
            try {
                val type =
                    objectMapper.typeFactory.constructParametricType(
                        SqsMessage::class.java,
                        EmailData::class.java,
                    )
                val message = objectMapper.readValue<SqsMessage<EmailData>>(record.body, type)

                val jobId = message.id
                val preventEmailJob = preventEmailJobRepository.findById(jobId)
                if (preventEmailJob != null) {
                    log.info("Prevent Email Job $jobId found")
                    return@forEach
                }

                val emailRecordId = sendEmail(message.data)
                log.info("Email sent to ${message.data.to}")
                result.add(emailRecordId)
                // Resend API has a limit of 2 requests per second.
                Thread.sleep(500)
            } catch (e: Exception) {
                log.error("Failed to process message: ${record.body}", e)
                throw e // 触发SQS重试
            }
        }
        return result
    }

    private fun sendEmail(emailData: EmailData): String =
        emailService.send(
            from = emailData.from,
            to = emailData.to,
            subject = emailData.subject,
            html = emailData.html,
        )
}
