package top.sunbath.api.email.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.context.ApplicationContext
import io.micronaut.core.annotation.Introspected
import io.micronaut.function.aws.MicronautRequestHandler
import org.slf4j.LoggerFactory
import top.sunbath.api.email.repository.PreventEmailJobRepository
import top.sunbath.api.email.service.EmailService
import top.sunbath.shared.types.EmailData
import top.sunbath.shared.types.SqsMessage

@Introspected
class EmailFunctionHandler : MicronautRequestHandler<SQSEvent, String>() {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val objectMapper = ObjectMapper()

    private lateinit var emailService: EmailService
    private lateinit var preventEmailJobRepository: PreventEmailJobRepository

    override fun getApplicationContext(): ApplicationContext {
        val ctx = super.getApplicationContext()
        emailService = ctx.getBean(EmailService::class.java)
        preventEmailJobRepository = ctx.getBean(PreventEmailJobRepository::class.java)
        return ctx
    }

    override fun execute(input: SQSEvent): String {
        input.records.forEach { record ->
            try {
                @Suppress("UNCHECKED_CAST")
                val message = objectMapper.readValue(record.body, SqsMessage::class.java) as SqsMessage<EmailData>
                val jobId = message.id
                val preventEmailJob = preventEmailJobRepository.findById(jobId)
                if (preventEmailJob != null) {
                    log.info("Prevent Email Job $jobId found")
                    return@forEach
                }

                sendEmail(message.data)
                log.info("Email sent to ${message.data.to}")
                // Resend API has a limit of 2 requests per second.
                Thread.sleep(500)
            } catch (e: Exception) {
                log.error("Failed to process message: ${record.body}", e)
                throw e // 触发SQS重试
            }
        }
        return "Executed"
    }

    private fun sendEmail(emailData: EmailData) {
        emailService.send(
            from = emailData.from,
            to = emailData.to,
            subject = emailData.subject,
            html = emailData.html,
        )
    }
}
