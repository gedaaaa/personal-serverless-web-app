package top.sunbath.api.auth.controller

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.uri.UriBuilder
import io.micronaut.validation.Validated
import jakarta.validation.Valid
import top.sunbath.api.auth.controller.request.CreateUserRequest
import top.sunbath.api.auth.model.User
import top.sunbath.api.auth.repository.UserRepository
import java.net.URI

/**
 * Controller for User CRUD operations.
 */
@Validated
@Controller("/users")
class UsersController(
    private val userRepository: UserRepository,
) {
    /**
     * Get all users.
     * @return List of all users
     */
    @Get
    fun index(): List<User> = userRepository.findAll()

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
}
