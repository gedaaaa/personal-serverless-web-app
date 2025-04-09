package top.sunbath.api.email.service

/**
 * Service interface for sending emails.
 */
interface EmailService {
    /**
     * Send a verification email to a user.
     *
     * @param to The recipient's email address
     * @param subject The subject of the email
     * @param html The HTML content of the email
     * @throws IllegalStateException if the email service is not properly configured
     * @throws RuntimeException if the email fails to send
     */
    fun send(
        from: String,
        to: String,
        subject: String,
        html: String,
    )
}
