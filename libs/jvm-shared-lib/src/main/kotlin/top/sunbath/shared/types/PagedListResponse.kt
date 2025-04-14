package top.sunbath.shared.types

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

/**
 * Response object for paginated list.
 */
@Introspected
@Serdeable
data class PagedListResponse<T>(
    val items: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
