package top.sunbath.api.email.model

import io.micronaut.core.annotation.Creator
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.NonNull
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.NotBlank
import top.sunbath.shared.dynamodb.Identified
import top.sunbath.shared.dynamodb.Indexable
import java.time.Instant

@Introspected
@Serdeable
class PreventEmailJob :
    Identified,
    Indexable {
    @get:NonNull
    override var id: String = ""

    @get:NonNull
    var createdAt: Instant? = Instant.now()

    constructor()

    @Creator
    constructor(
        @NonNull @NotBlank id: String,
    ) {
        this.id = id
    }

    override fun getIndexValues(): Map<String, String> = mutableMapOf<String, String>()
}
