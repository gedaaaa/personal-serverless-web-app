package top.sunbath.api.email.integration

import top.sunbath.shared.types.EmailData

object TestEmailDataFactory {
    fun createEmailData() =
        EmailData(
            from = "noreply@example.com",
            to = "test@example.com",
            subject = "Test Subject",
            html = "<p>Test Content</p>",
        )
}
