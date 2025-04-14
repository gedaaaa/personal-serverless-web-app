package top.sunbath.api.memo.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank
import java.time.Instant

/**
 * Request object for creating a new memo.
 */
@Introspected
@Serdeable
data class CreateMemoRequest(
    @field:NotBlank
    val title: String,
    @field:NotBlank
    val content: String,
    @field:Nullable
    val reminderTime: Instant?,
)
