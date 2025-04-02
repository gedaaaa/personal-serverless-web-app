package top.sunbath.api.auth.service.email

import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Email template manager.
 */
object EmailTemplate {
    private val dateFormatter =
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())

    /**
     * Generate verification email content.
     *
     * @param username The recipient's username
     * @param verificationToken The verification token
     * @param expiresAt When the verification token expires
     * @return A pair of subject and HTML content
     */
    fun generateVerificationEmail(
        username: String,
        verificationToken: String,
        expiresAt: Instant,
    ): Pair<String, String> {
        val subject = "Verify your email address"
        val escapedToken = URLEncoder.encode(verificationToken, "UTF-8")
        val verificationLink = "https://sunbath.top/auth/verify-email?token=$escapedToken"
        val expiresAtFormatted = dateFormatter.format(expiresAt)

        val html =
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Email Verification</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2>Welcome to Sunbath!</h2>
                    <p>Hi $username,</p>
                    <p>Thank you for registering. Please verify your email address by clicking the button below:</p>
                    <p style="text-align: center; margin: 30px 0;">
                        <a href="$verificationLink" 
                           style="background-color: #4CAF50; color: white; padding: 12px 24px; 
                                  text-decoration: none; border-radius: 4px; display: inline-block;">
                            Verify Email
                        </a>
                    </p>
                    <p>Or copy and paste this link into your browser:</p>
                    <p style="background-color: #f5f5f5; padding: 10px; word-break: break-all;">
                        $verificationLink
                    </p>
                    <p>This verification link will expire at: $expiresAtFormatted</p>
                    <p>If you didn't create an account, you can safely ignore this email.</p>
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #eee;">
                    <p style="font-size: 12px; color: #666;">
                        This is an automated message, please do not reply.
                    </p>
                </div>
            </body>
            </html>
            """.trimIndent()

        return subject to html
    }
}
