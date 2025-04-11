package top.sunbath.api.auth.controller

import io.micronaut.http.HttpStatus
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import top.sunbath.api.auth.controller.request.ResendVerificationEmailRequest
import top.sunbath.api.auth.controller.request.VerifyEmailRequest
import top.sunbath.api.auth.service.AuthService

/**
 * Unit tests for the EmailVerificationController.
 */
@ExtendWith(MockKExtension::class)
class EmailVerificationControllerTest {
    @MockK
    private lateinit var authService: AuthService

    private lateinit var controller: EmailVerificationController

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        controller = EmailVerificationController(authService)
    }

    @Test
    fun `test successful email verification`() {
        // Given
        val token = "valid_token"
        val request = VerifyEmailRequest(token)
        val successMessage = "Email verified successfully"

        every { authService.verifyEmail(token) } returns successMessage

        // When
        val response = controller.verifyEmail(request)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertEquals(successMessage, response.body()?.message)

        verify(exactly = 1) { authService.verifyEmail(token) }
    }

    @Test
    fun `test successful resend verification email`() {
        // Given
        val email = "test@example.com"
        val request = ResendVerificationEmailRequest(email)
        val successMessage = "Verification email sent successfully"

        every { authService.resendVerificationEmail(email) } returns successMessage

        // When
        val response = controller.resendVerificationEmail(request)

        // Then
        assertEquals(HttpStatus.OK, response.status)
        assertEquals(successMessage, response.body()?.message)

        verify(exactly = 1) { authService.resendVerificationEmail(email) }
    }
}
