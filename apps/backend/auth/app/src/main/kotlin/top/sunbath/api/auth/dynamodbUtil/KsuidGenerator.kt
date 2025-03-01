package top.sunbath.api.auth.dynamodbUtil

import com.github.ksuid.Ksuid
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton

@Requires(classes = [Ksuid::class])
@Singleton
class KsuidGenerator : IdGenerator {
    @NonNull
    override fun generate(): String = Ksuid.newKsuid().toString()
}
