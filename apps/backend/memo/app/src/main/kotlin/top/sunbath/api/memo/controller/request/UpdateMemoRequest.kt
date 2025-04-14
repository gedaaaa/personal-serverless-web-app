package top.sunbath.api.memo.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank
import java.time.Instant

/**
 * Request object for updating an existing user.
 */
@Introspected
@Serdeable
data class UpdateMemoRequest(
    @field:NotBlank
    val title: String?,
    @field:NotBlank
    val content: String?,
    @field:Nullable
    val reminderTime: Instant?,
    @field:NotBlank
    val isCompleted: Boolean?,
)
