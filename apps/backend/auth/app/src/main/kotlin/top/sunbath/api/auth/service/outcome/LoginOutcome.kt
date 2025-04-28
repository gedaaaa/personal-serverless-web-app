package top.sunbath.api.auth.service.outcome

import io.micronaut.serde.annotation.Serdeable

/**
 * Represents the possible outcomes of a login attempt.
 */
@Serdeable
sealed interface LoginOutcome {
    /**
     * Login was successful.
     * @param jwt The generated JSON Web Token.
     */
    @Serdeable
    data class Success(
        val token: String,
    ) : LoginOutcome

    /**
     * Login requires password migration.
     * @param migrationToken A one-time token required to complete the migration.
     */
    @Serdeable
    data class MigrationRequired(
        val migrationToken: String,
    ) : LoginOutcome
}
