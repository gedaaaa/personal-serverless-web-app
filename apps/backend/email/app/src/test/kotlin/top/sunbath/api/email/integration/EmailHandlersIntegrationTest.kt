package top.sunbath.api.email.integration

import io.micronaut.context.ApplicationContext
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest
import top.sunbath.api.email.handler.CancelEmailFunctionHandler
import top.sunbath.api.email.handler.EmailFunctionHandler
import top.sunbath.api.email.integration.TestEmailDataFactory
import top.sunbath.api.email.integration.TestMessageFactory
import top.sunbath.api.email.repository.EmailRecordRepository
import top.sunbath.api.email.repository.PreventEmailJobRepository
import top.sunbath.api.email.service.EmailService
import top.sunbath.shared.dynamodb.DynamoConfiguration
import java.util.UUID

/**
 * TestEmailFunctionHandler is a test class that extends EmailFunctionHandler.
 *
 * EmailFunctionHandler requires non-argument constructor,
 *  so the bean injection is done in the getApplicationContext() method.
 *
 * By default, Micronaut will create a dedicated ApplicationContext,
 *  which is not same as the one in the test environment.
 *
 * This class overrides the getApplicationContext() method to inject test ApplicationContext into the handler.
 */
class TestEmailFunctionHandler(
    private val ctx: ApplicationContext,
) : EmailFunctionHandler() {
    override fun getApplicationContext(): ApplicationContext {
        val context = ctx
        println("context: $context")
        emailService = context.getBean(EmailService::class.java)
        preventEmailJobRepository = context.getBean(PreventEmailJobRepository::class.java)
        return context
    }
}

/**
 * TestCancelEmailFunctionHandler is a test class that extends CancelEmailFunctionHandler.
 *
 * CancelEmailFunctionHandler requires non-argument constructor,
 *  so the bean injection is done in the getApplicationContext() method.
 *
 * By default, Micronaut will create a dedicated ApplicationContext,
 *  which is not same as the one in the test environment.
 *
 * This class overrides the getApplicationContext() method to inject test ApplicationContext into the handler.
 */
class TestCancelEmailFunctionHandler(
    private val ctx: ApplicationContext,
) : CancelEmailFunctionHandler() {
    override fun getApplicationContext(): ApplicationContext {
        val context = ctx
        preventEmailJobRepository = context.getBean(PreventEmailJobRepository::class.java)
        return context
    }
}

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailHandlersIntegrationTest {
    @Inject
    private lateinit var applicationContext: ApplicationContext

    @Inject
    private lateinit var emailRecordRepository: EmailRecordRepository

    @Inject
    private lateinit var preventEmailJobRepository: PreventEmailJobRepository

    @Inject
    private lateinit var dynamoConfiguration: DynamoConfiguration

    @Inject
    private lateinit var dynamoDbClient: DynamoDbClient

    private lateinit var emailHandler: EmailFunctionHandler

    private lateinit var cancelHandler: CancelEmailFunctionHandler

    private val testEmailData = TestEmailDataFactory.createEmailData()

    @BeforeAll
    fun setup() {
        // Inject test ApplicationContext into the handlers, and call getApplicationContext() to initialize the beans
        emailHandler = TestEmailFunctionHandler(applicationContext)
        emailHandler.getApplicationContext()
        cancelHandler = TestCancelEmailFunctionHandler(applicationContext)
        cancelHandler.getApplicationContext()
    }

    @AfterAll
    fun tearDown() {
        // clean database
        dynamoDbClient.deleteTable(DeleteTableRequest.builder().tableName(dynamoConfiguration.tableName).build())
    }

    @Test
    fun `should send email when message is not in block list`() {
        // Given
        val messageId = UUID.randomUUID().toString()
        val message = TestMessageFactory.createEmailMessage(messageId, testEmailData)

        // When
        val emailRecordIds = emailHandler.execute(message)

        // Then
        assertEquals(1, emailRecordIds.size)
        val emailRecord = emailRecordRepository.findById(emailRecordIds[0])
        assertNotNull(emailRecord)
        assertEquals(testEmailData.to, emailRecord!!.to)
        assertEquals(testEmailData.from, emailRecord.from)
        assertEquals(testEmailData.subject, emailRecord.subject)
        assertEquals(testEmailData.html, emailRecord.html)
    }

    @Test
    fun `should not send email when message is in block list`() {
        // Given
        val messageId = UUID.randomUUID().toString()
        val cancelMessage = TestMessageFactory.createCancelMessage(messageId)
        val emailMessage = TestMessageFactory.createEmailMessage(messageId, testEmailData)

        // When
        cancelHandler.execute(cancelMessage)
        val emailRecordIds = emailHandler.execute(emailMessage)

        // Then
        assertEquals(0, emailRecordIds.size)
        assertNotNull(preventEmailJobRepository.findById(messageId))

        // verify no email is sent
    }

    @Test
    fun `should handle message order - email sent before cancel`() {
        // Given
        val messageId = UUID.randomUUID().toString()
        val emailMessage = TestMessageFactory.createEmailMessage(messageId, testEmailData)
        val cancelMessage = TestMessageFactory.createCancelMessage(messageId)

        // When
        val emailRecordIds = emailHandler.execute(emailMessage)
        cancelHandler.execute(cancelMessage)

        // Then
        assertEquals(1, emailRecordIds.size)
        val emailRecord = emailRecordRepository.findById(emailRecordIds[0])
        assertNotNull(emailRecord)
        assertEquals(testEmailData.to, emailRecord!!.to)
        assertEquals(testEmailData.from, emailRecord.from)
        assertEquals(testEmailData.subject, emailRecord.subject)
        assertEquals(testEmailData.html, emailRecord.html)
        assertNotNull(preventEmailJobRepository.findById(messageId))
    }

    @Test
    fun `should handle duplicate cancel requests`() {
        // Given
        val messageId = UUID.randomUUID().toString()
        val cancelMessage = TestMessageFactory.createCancelMessage(messageId)

        // When & Then
        assertDoesNotThrow {
            cancelHandler.execute(cancelMessage)
            cancelHandler.execute(cancelMessage)
        }
        assertNotNull(preventEmailJobRepository.findById(messageId))
    }

    @Test
    fun `should throw exception for invalid message format`() {
        // Given
        val invalidMessage = TestMessageFactory.createInvalidMessage()

        // When & Then
        assertThrows(Exception::class.java) {
            emailHandler.execute(invalidMessage)
        }
    }

    @Test
    fun `should handle resend api error`() {
        // Given
        val messageId = UUID.randomUUID().toString()
        val errorEmailData = testEmailData.copy(from = "error@test.com")
        val message = TestMessageFactory.createEmailMessage(messageId, errorEmailData)

        // When
        val emailRecordIds = emailHandler.execute(message)

        // Then
        assertEquals(1, emailRecordIds.size)
        val emailRecord = emailRecordRepository.findById(emailRecordIds[0])
        assertNotNull(emailRecord?.vendorResponse)
        assertTrue(emailRecord?.vendorResponse?.contains("Test error") ?: false)
    }
}
