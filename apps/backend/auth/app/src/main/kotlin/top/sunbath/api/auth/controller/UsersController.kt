package top.sunbath.api.auth.controller

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
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.controller.request.UpdateUserRequest
import top.sunbath.api.auth.controller.response.PagedUsersResponse
import top.sunbath.api.auth.model.User
import top.sunbath.api.auth.repository.UserRepository
import java.net.URI

/**
 * Controller for User CRUD operations.
 */
@Validated
@Controller("/users")
@Secured("ROLE_ADMIN")
class UsersController(
    private val userRepository: UserRepository,
) {
    /**
     * Get all users with pagination.
     * @param limit The maximum number of users to return (default: 10)
     * @param cursor The cursor for pagination (null for first page)
     * @return Paginated list of users
     */
    @Get
    fun index(
        @QueryValue(defaultValue = "10") @Max(100) limit: Int,
        @QueryValue()@Nullable() cursor: String?,
    ): PagedUsersResponse {
        val (users, nextCursor) = userRepository.findAllWithCursor(limit, cursor)
        return PagedUsersResponse(
            users = users,
            nextCursor = nextCursor,
            hasMore = nextCursor != null,
        )
    }

    /**
     * Get a user by ID.
     * @param id The user ID
     * @return The user if found
     */
    @Get("/{id}")
    fun show(
        @PathVariable id: String,
    ): HttpResponse<User> {
        val user = userRepository.findById(id)
        return if (user != null) {
            HttpResponse.ok(user)
        } else {
            HttpResponse.notFound()
        }
    }

    /**
     * Create a new user.
     * @param request The create user request
     * @return HTTP response with location header
     */
    @Post
    fun save(
        @Body @Valid request: CreateUserRequest,
    ): HttpResponse<Void> {
        val id =
            userRepository.save(
                request.username,
                request.email,
                request.password,
                setOf("ROLE_USER"),
                request.fullName,
            )
        val uri: URI =
            UriBuilder
                .of("/users")
                .path(id)
                .build()
        return HttpResponse.created(uri)
    }

    /**
     * Delete a user by ID.
     * @param id The user ID
     * @return HTTP response
     */
    @Delete("/{id}")
    fun delete(
        @PathVariable id: String,
    ): HttpResponse<Void> {
        userRepository.delete(id)
        return HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    /**
     * Update a user by ID.
     * @param id The user ID
     * @param request The update user request
     * @return HTTP response
     */
    @Put("/{id}")
    fun update(
        @PathVariable id: String,
        @Body @Valid request: UpdateUserRequest,
    ): HttpResponse<Void> {
        val updated =
            userRepository.update(
                id,
                request.email,
                request.password,
                request.roles,
                request.fullName,
            )

        return if (updated) {
            HttpResponse.noContent()
        } else {
            HttpResponse.notFound()
        }
    }
}
