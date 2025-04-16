package top.sunbath.shared.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

@Introspected
@Serdeable
data class SqsMessage<T>
    @JsonCreator
    constructor(
        @JsonProperty("id") val id: String,
        @JsonProperty("data") val data: T,
    )
