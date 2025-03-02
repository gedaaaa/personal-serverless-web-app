package top.sunbath.api.auth.controller.response

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable
import top.sunbath.api.auth.model.User

/**
 * Response object for paginated users list.
 */
@Introspected
@Serdeable
data class PagedUsersResponse(
    val users: List<User>,
    val nextCursor: String?,
    val hasMore: Boolean,
) 
