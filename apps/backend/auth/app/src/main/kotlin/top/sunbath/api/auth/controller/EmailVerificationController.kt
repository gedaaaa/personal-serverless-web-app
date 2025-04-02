package top.sunbath.api.auth.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import top.sunbath.api.auth.controller.request.ResendVerificationEmailRequest
import top.sunbath.api.auth.controller.request.VerifyEmailRequest
import top.sunbath.api.auth.controller.response.VerifyEmailResponse
import top.sunbath.api.auth.service.AuthService

/**
 * Controller for email verification operations.
 */
@Validated
@Controller("/email-verification")
@Secured(SecurityRule.IS_ANONYMOUS)
class EmailVerificationController(
    private val authService: AuthService,
) {
    /**
     * Verify email with the provided token.
     * @param request The verification request containing the token
     * @return HTTP response with verification result
     */
    @Post("/token")
    fun verifyEmail(
        @Body @Valid request: VerifyEmailRequest,
    ): HttpResponse<VerifyEmailResponse> {
        val result = authService.verifyEmail(request.token)
        return HttpResponse.ok(VerifyEmailResponse(result))
    }

    /**
     * Resend verification email to the provided email address.
     * @param request The request containing the email address
     * @return HTTP response with operation result
     */
    @Post("/resend-email")
    fun resendVerificationEmail(
        @Body @Valid request: ResendVerificationEmailRequest,
    ): HttpResponse<VerifyEmailResponse> {
        val result = authService.resendVerificationEmail(request.email)
        return HttpResponse.ok(VerifyEmailResponse(result))
    }
}
