package top.sunbath.api.email.integration
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.databind.ObjectMapper
import top.sunbath.shared.types.EmailData
import top.sunbath.shared.types.SqsMessage

object TestMessageFactory {
    private val objectMapper = ObjectMapper()

    fun createEmailMessage(
        messageId: String,
        emailData: EmailData,
    ): SQSEvent {
        val sqsMessage = SqsMessage<EmailData>(id = messageId, data = emailData)
        val messageBody = objectMapper.writeValueAsString(sqsMessage)

        return SQSEvent().apply {
            records =
                listOf(
                    SQSEvent.SQSMessage().apply {
                        body = messageBody
                    },
                )
        }
    }

    fun createCancelMessage(messageId: String): SQSEvent {
        val sqsMessage = SqsMessage<String>(id = messageId, data = messageId)
        val messageBody = objectMapper.writeValueAsString(sqsMessage)

        return SQSEvent().apply {
            records =
                listOf(
                    SQSEvent.SQSMessage().apply {
                        body = messageBody
                    },
                )
        }
    }

    fun createInvalidMessage(): SQSEvent =
        SQSEvent().apply {
            records =
                listOf(
                    SQSEvent.SQSMessage().apply {
                        body = "invalid message body"
                    },
                )
        }
}
