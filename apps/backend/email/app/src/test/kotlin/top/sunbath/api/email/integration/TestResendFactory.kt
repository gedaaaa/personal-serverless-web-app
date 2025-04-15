package top.sunbath.api.email.integration

import com.resend.Resend
import com.resend.core.exception.ResendException
import com.resend.services.emails.model.CreateEmailOptions
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Singleton
import top.sunbath.api.email.config.ResendApiKeyProvider
import top.sunbath.shared.ssm.SsmParameterProvider

@Factory
class TestResendFactory {
    @Singleton
    @Replaces(Resend::class)
    fun mockResend(): Resend =
        mockk {
            every { emails() } returns
                mockk {
                    every {
                        send(
                            match { options: CreateEmailOptions ->
                                options.from != "error@test.com"
                            },
                        )
                    } returns mockk(relaxed = true)

                    every {
                        send(
                            match { options: CreateEmailOptions ->
                                options.from == "error@test.com"
                            },
                        )
                    } throws ResendException("Test error")
                }
        }

    @Singleton
    @Replaces(ResendApiKeyProvider::class)
    fun mockResendApiKeyProvider(): ResendApiKeyProvider =
        mockk {
            every { getApiKey() } returns "test-api-key"
        }

    @Singleton
    @Replaces(SsmParameterProvider::class)
    fun mockSsmParameterProvider(): SsmParameterProvider =
        mockk {
            every { getParameter(any()) } returns "test-parameter-value"
            every { getParameterOrNull(any()) } returns "test-parameter-value"
        }
}
