package top.sunbath.shared.types

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

@Introspected
@Serdeable
data class EmailData
    @JsonCreator
    constructor(
        @JsonProperty("to") val to: String,
        @JsonProperty("subject") val subject: String,
        @JsonProperty("html") val html: String,
        @JsonProperty("from") val from: String = "no-reply@sunbath.top",
    )
