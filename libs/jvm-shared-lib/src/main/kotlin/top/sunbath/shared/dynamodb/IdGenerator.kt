package top.sunbath.shared.dynamodb

import io.micronaut.core.annotation.NonNull

@FunctionalInterface
interface IdGenerator {
    @NonNull
    fun generate(): String
}
