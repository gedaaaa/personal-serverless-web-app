package top.sunbath.api.memo.service.notification

import top.sunbath.api.memo.model.Memo
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
    fun generateMemoNotificationEmail(memo: Memo): Pair<String, String> {
        val subject = "Reminder of Your Memo: [${if (memo.title.isEmpty()) "Untitled Memo" else memo.title}]"

        val html =
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>$subject</title>
            </head>
            <body style="margin: 0; padding: 20px; font-family: Arial, sans-serif;">
                <div style="max-width: 600px; margin: auto;">
                    <h1 style="color: #2c3e50; margin-bottom: 30px;">Memo Reminder</h1>
                    
                    <div style="background: #f8f9fa; border-radius: 8px; padding: 20px; margin-bottom: 25px; border: 1px solid #e0e0e0;">
                        <h2 style="color: #2c3e50; margin-top: 0; font-size: 20px;">
                            ${if (memo.title.isEmpty()) "Untitled Memo" else memo.title}
                        </h2>
                        <div style="color: #555; line-height: 1.6; margin-bottom: 15px;">
                            ${memo.content.replace("\n", "<br>")}
                        </div>
                        <div style="color: #7f8c8d; font-size: 14px;">
                            ⏰ Due: ${dateFormatter.format(memo.reminderTime)}
                        </div>
                    </div>

                    <p style="color: #666; line-height: 1.6; margin-bottom: 25px;">
                        This is a friendly reminder about your memo. You can view and manage all your memos on our platform:
                    </p>
                    
                    <table role="presentation" cellspacing="0" cellpadding="0" style="margin: 30px 0;">
                        <tr>
                            <td style="background: #9333ea; border-radius: 5px; text-align: center;">
                                <a href="https://sunbath.top/memo" 
                                   style="display: inline-block; padding: 12px 25px;
                                          border-radius: 8px;
                                          color: #ffffff; text-decoration: none; 
                                          font-weight: bold; font-size: 15px;">
                                    View All Memos
                                </a>
                            </td>
                        </tr>
                    </table>

                    <div style="border-top: 1px solid #eee; padding-top: 20px; margin-top: 30px; 
                                color: #95a5a6; font-size: 12px;">
                        <p>This is an automated message. Please do not reply directly to this email.</p>
                    </div>
                </div>
            </body>
            </html>
            """.trimIndent()

        return subject to html
    }
}
