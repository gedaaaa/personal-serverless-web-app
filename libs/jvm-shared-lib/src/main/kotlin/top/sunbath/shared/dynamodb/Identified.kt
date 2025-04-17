package top.sunbath.shared.dynamodb

import io.micronaut.core.annotation.NonNull

/**
 * Interface for entities with an ID.
 */
interface Identified {
    /**
     * The entity ID
     */
    @get:NonNull
    val id: String
}
