package top.sunbath.api.email.repository

import io.micronaut.core.annotation.NonNull
import top.sunbath.api.email.model.PreventEmailJob

/**
 * Repository interface for User entity operations.
 */
interface PreventEmailJobRepository {
    /**
     * Save a new prevent email job.
     */
    @NonNull
    fun save(id: String): String

    /**
     * Find a prevent email job by id.
     */
    @NonNull
    fun findById(id: String): PreventEmailJob?
}
