package top.sunbath.api.auth.model

import io.micronaut.core.annotation.Creator
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import top.sunbath.shared.dynamodb.Identified
import top.sunbath.shared.dynamodb.Indexable
import java.time.Instant

/**
 * A User entity.
 */
@Introspected
@Serdeable
class User :
    Identified,
    Indexable {
    @get:NonNull
    override var id: String = ""

    @get:NonNull
    var username: String = ""

    @get:NonNull
    var email: String = ""

    @get:NonNull
    var password: String = ""

    @get:NonNull
    var roles: Set<String> = setOf("ROLE_USER")

    @get:Nullable
    var fullName: String? = null

    @get:NonNull
    var emailVerified: Boolean = false

    @get:Nullable
    var emailVerificationToken: String? = null

    @get:Nullable
    var emailVerificationTokenExpiresAt: Instant? = null

    @get:Nullable
    var lastVerificationEmailSentAt: Instant? = null

    /**
     * Default constructor.
     */
    constructor()

    /**
     * Constructor with all properties.
     * @param id The user ID
     * @param username The username
     * @param email The email
     * @param password The hashed password
     * @param roles The user roles
     * @param fullName The full name
     * @param emailVerified Whether the email is verified
     * @param emailVerificationToken The email verification token
     * @param emailVerificationTokenExpiresAt When the verification token expires
     * @param lastVerificationEmailSentAt When the last verification email was sent
     */
    @Creator
    constructor(
        id: String,
        username: String,
        email: String,
        password: String,
        roles: Set<String> = setOf("ROLE_USER"),
        fullName: String? = null,
        emailVerified: Boolean = false,
        emailVerificationToken: String? = null,
        emailVerificationTokenExpiresAt: Instant? = null,
        lastVerificationEmailSentAt: Instant? = null,
    ) {
        this.id = id
        this.username = username
        this.email = email
        this.password = password
        this.roles = roles
        this.fullName = fullName
        this.emailVerified = emailVerified
        this.emailVerificationToken = emailVerificationToken
        this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt
        this.lastVerificationEmailSentAt = lastVerificationEmailSentAt
    }

    /**
     * Returns index values for this user.
     */
    override fun getIndexValues(): Map<String, String> {
        val indexValues = mutableMapOf<String, String>()

        // Add username index values - use the same constants as in DefaultUserRepository
        username.let {
            indexValues["USERNAME_PK"] = it
            indexValues["USERNAME_SK"] = id
        }

        // Add email index values
        email.let {
            indexValues["EMAIL_PK"] = it
            indexValues["EMAIL_SK"] = id
        }

        // Add verification token index if exists
        emailVerificationToken?.let {
            indexValues["VERIFICATION_TOKEN_PK"] = it
            indexValues["VERIFICATION_TOKEN_SK"] = id
        }

        return indexValues
    }

    /**
     * Check if the verification token is valid.
     * @return true if the token is valid and not expired
     */
    fun isVerificationTokenValid(): Boolean =
        emailVerificationToken != null &&
            emailVerificationTokenExpiresAt != null &&
            emailVerificationTokenExpiresAt!!.isAfter(Instant.now())

    /**
     * Check if a verification email can be sent based on the minimum interval.
     * @param minIntervalSeconds The minimum interval in seconds between verification emails
     * @return true if a verification email can be sent
     */
    fun canSendVerificationEmail(minIntervalSeconds: Long): Boolean =
        lastVerificationEmailSentAt == null ||
            lastVerificationEmailSentAt!!.plusSeconds(minIntervalSeconds).isBefore(Instant.now())

    /**
     * Update the verification token and its expiration time.
     * @param token The new verification token
     * @param expiresInSeconds The number of seconds until the token expires
     */
    fun updateVerificationToken(
        token: String,
        expiresInSeconds: Long,
    ) {
        emailVerificationToken = token
        emailVerificationTokenExpiresAt = Instant.now().plusSeconds(expiresInSeconds)
        lastVerificationEmailSentAt = Instant.now()
    }

    /**
     * Mark the email as verified and clear the verification token.
     */
    fun markEmailAsVerified() {
        emailVerified = true
        emailVerificationToken = null
        emailVerificationTokenExpiresAt = null
    }
}
