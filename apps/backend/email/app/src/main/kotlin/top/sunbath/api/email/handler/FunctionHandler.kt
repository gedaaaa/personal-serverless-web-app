package top.sunbath.api.email.handler

import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.micronaut.core.annotation.Introspected
import io.micronaut.function.aws.MicronautRequestHandler
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory
import top.sunbath.api.email.service.EmailService
import top.sunbath.shared.types.EmailData

@Introspected
@Singleton
class FunctionHandler(
    private val emailService: EmailService,
) : MicronautRequestHandler<SQSEvent, Unit>() {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val objectMapper = ObjectMapper()

    override fun execute(input: SQSEvent) {
        input.records.forEach { record ->
            try {
                val emailData = objectMapper.readValue(record.body, EmailData::class.java)
                sendEmail(emailData)
                log.info("Email sent to ${emailData.to}")
                // Resend API has a limit of 2 requests per second.
                Thread.sleep(500)
            } catch (e: Exception) {
                log.error("Failed to process message: ${record.body}", e)
                throw e // 触发SQS重试
            }
        }
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
