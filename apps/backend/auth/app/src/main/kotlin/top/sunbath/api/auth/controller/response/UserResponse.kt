package top.sunbath.api.auth.controller.response

import top.sunbath.api.auth.model.User
import java.time.Instant

data class UserResponse(
    val id: String,
    val username: String,
    val email: String,
    val roles: Set<String>,
    val fullName: String?,
    val emailVerified: Boolean,
    val emailVerificationTokenExpiresAt: Instant?,
) {
    constructor(user: User) : this(
        id = user.id,
        username = user.username,
        email = user.email,
        roles = user.roles,
        fullName = user.fullName,
        emailVerified = user.emailVerified,
        emailVerificationTokenExpiresAt = user.emailVerificationTokenExpiresAt,
    )
}
