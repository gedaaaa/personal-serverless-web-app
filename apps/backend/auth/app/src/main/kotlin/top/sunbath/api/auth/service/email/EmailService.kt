package top.sunbath.api.auth.service.email

/**
 * Service interface for sending emails.
 */
interface EmailService {
    /**
     * Send a verification email to a user.
     *
     * @param to The recipient's email address
     * @param username The recipient's username
     * @param verificationToken The verification token
     * @param expiresAt When the verification token expires
     * @throws IllegalStateException if the email service is not properly configured
     * @throws RuntimeException if the email fails to send
     */
    fun sendVerificationEmail(
        to: String,
        username: String,
        verificationToken: String,
        expiresAt: java.time.Instant,
    )
}
