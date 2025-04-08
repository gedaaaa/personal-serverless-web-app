package top.sunbath.shared.types

import io.micronaut.core.annotation.Introspected

@Introspected
data class EmailData(
    val to: String,
    val subject: String,
    val html: String,
    val from: String = "no-reply@sunbath.top",
)
