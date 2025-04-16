package top.sunbath.api.memo.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * Request object for creating a new memo.
 */
@Introspected
@Serdeable
data class CreateMemoRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    @field:NotNull
    val title: String,
    @field:NotBlank
    @field:Size(min = 1, max = 1000)
    @field:NotNull
    val content: String,
    @field:Nullable
    val reminderTime: Instant?,
)
