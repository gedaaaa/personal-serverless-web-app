package top.sunbath.api.auth.dynamodbUtil

import io.micronaut.core.annotation.NonNull

/**
 * Interface for entities with an ID.
 */
interface Identified {
    /**
     * The entity ID
     */
    @get:NonNull
    val id: String?
}
