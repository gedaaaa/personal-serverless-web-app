package top.sunbath.api.auth.model

import io.micronaut.core.annotation.Creator
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import top.sunbath.api.auth.dynamodbUtil.Identified

/**
 * A User entity.
 */
@Introspected
@Serdeable
class User : Identified {
    @get:NonNull
    override var id: String? = null

    @get:NonNull
    var username: String? = null

    @get:NonNull
    var email: String? = null

    @get:Nullable
    var fullName: String? = null

    /**
     * Default constructor.
     */
    constructor()

    /**
     * Constructor with all properties.
     * @param id The user ID
     * @param username The username
     * @param email The email
     * @param fullName The full name
     */
    @Creator
    constructor(
        id: String,
        username: String,
        email: String,
        fullName: String?,
    ) {
        this.id = id
        this.username = username
        this.email = email
        this.fullName = fullName
    }
}
