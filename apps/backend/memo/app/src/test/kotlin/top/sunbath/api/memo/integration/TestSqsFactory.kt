package top.sunbath.api.email.integration

import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Singleton
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import top.sunbath.shared.sqs.SqsConfiguration
import top.sunbath.shared.ssm.SsmParameterProvider

// TODO: change to notification service factory
@Factory
class TestSqsFactory {
    @Singleton
    @Primary
    fun mockSqsClient(): SqsClient =
        mockk {
            every { getQueueUrl(any<GetQueueUrlRequest>()) } returns
                mockk<GetQueueUrlResponse> {
                    every { queueUrl() } returns "test-queue-url"
                }

            every { sendMessage(any<SendMessageRequest>()) } returns
                mockk<SendMessageResponse>(relaxed = true)
        }

    @Singleton
    @Replaces(SqsConfiguration::class)
    fun mockSqsConfiguration(): SqsConfiguration =
        mockk<SqsConfiguration> {
            every { getQueues() } returns mapOf("email" to "test-queue-url", "cancel-email" to "test-cancel-queue-url")
        }

    @Singleton
    @Replaces(SsmParameterProvider::class)
    fun mockSsmParameterProvider(): SsmParameterProvider =
        mockk {
            every { getParameter(any()) } returns "test-parameter-value"
            every { getParameterOrNull(any()) } returns "test-parameter-value"
        }
}
