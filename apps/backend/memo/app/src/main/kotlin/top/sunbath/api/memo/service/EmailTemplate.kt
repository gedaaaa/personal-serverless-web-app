package top.sunbath.api.memo.service

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
        val subject = "[${if (memo.title.isEmpty()) "Untitled Memo" else memo.title}] Needs Your Attention"

        val html =
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>$subject</title>
            </head>
            <body>
                <h1>$subject</h1>
                <p>${memo.content}</p>
                <p>Due at ${dateFormatter.format(memo.reminderTime)}</p>
            </body>
            </html>
            """.trimIndent()

        return subject to html
    }
}
