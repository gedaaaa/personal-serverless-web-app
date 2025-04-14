package top.sunbath.api.memo.controller

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.uri.UriBuilder
import io.micronaut.security.annotation.Secured
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import top.sunbath.api.memo.controller.request.CreateMemoRequest
import top.sunbath.api.memo.controller.request.UpdateMemoRequest
import top.sunbath.api.memo.model.Memo
import top.sunbath.api.memo.service.MemoService
import top.sunbath.shared.security.currentUser.CurrentUser
import top.sunbath.shared.types.PagedListResponse
import top.sunbath.shared.types.UserInfo
import java.net.URI

/**
 * Controller for Memo CRUD operations.
 */
@Validated
@Controller("")
@Secured("ROLE_USER")
class MemoController(
    private val memoService: MemoService,
) {
    /**
     * Get all memos with pagination.
     * @param limit The maximum number of memos to return (default: 10)
     * @param cursor The cursor for pagination (null for first page)
     * @return Paginated list of memos
     */
    @Get
    fun index(
        @QueryValue(defaultValue = "10") @Max(100) limit: Int,
        @QueryValue()@Nullable() cursor: String?,
        @CurrentUser userInfo: UserInfo,
    ): PagedListResponse<Memo> {
        val (memos, nextCursor) = memoService.getAllMemosWithCursor(userInfo, limit, cursor)
        return PagedListResponse(
            items = memos,
            nextCursor = nextCursor,
            hasMore = nextCursor != null,
        )
    }

    /**
     * Get a memo by ID.
     * @param id The memo ID
     * @return The memo if found
     */
    @Get("/{id}")
    fun show(
        @CurrentUser userInfo: UserInfo,
        @PathVariable id: String,
    ): HttpResponse<Memo> {
        val memo = memoService.getMemoById(userInfo, id)
        return if (memo != null) {
            HttpResponse.ok(memo)
        } else {
            HttpResponse.notFound()
        }
    }

    /**
     * Create a new memo.
     * @param request The create memo request
     * @return HTTP response with location header
     */
    @Post
    fun save(
        @CurrentUser userInfo: UserInfo,
        @Body @Valid request: CreateMemoRequest,
    ): HttpResponse<Void> {
        val id =
            memoService.createMemo(
                userInfo = userInfo,
                title = request.title,
                content = request.content,
                reminderTime = request.reminderTime,
            )
        val uri: URI =
            UriBuilder
                .of("/memos")
                .path(id)
                .build()
        return HttpResponse.created(uri)
    }

    /**
     * Delete a memo by ID.
     * @param id The memo ID
     * @return HTTP response
     */
    @Delete("/{id}")
    fun delete(
        @CurrentUser userInfo: UserInfo,
        @PathVariable id: String,
    ): HttpResponse<Void> {
        memoService.deleteMemo(userInfo, id)
        return HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    /**
     * Update a memo by ID.
     * @param id The memo ID
     * @param request The update memo request
     * @return HTTP response with updated memo data
     */
    @Put("/{id}")
    fun update(
        @CurrentUser userInfo: UserInfo,
        @PathVariable id: String,
        @Body @Valid request: UpdateMemoRequest,
    ): HttpResponse<Memo> {
        val updateResult =
            memoService.updateMemo(
                userInfo = userInfo,
                id = id,
                title = request.title,
                content = request.content,
                reminderTime = request.reminderTime,
                isCompleted = request.isCompleted,
                isDeleted = null,
            )

        return if (updateResult) {
            HttpResponse.ok(memoService.getMemoById(userInfo, id) ?: return HttpResponse.notFound())
        } else {
            HttpResponse.notFound()
        }
    }
}
