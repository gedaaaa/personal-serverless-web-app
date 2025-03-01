package top.sunbath.api.auth.dynamodbUtil

import io.micronaut.core.annotation.NonNull

@FunctionalInterface
interface IdGenerator {
    @NonNull
    fun generate(): String
}
