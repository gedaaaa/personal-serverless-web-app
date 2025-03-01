package top.sunbath.api.auth.config

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Requires
import jakarta.validation.constraints.NotBlank

@Requires(property = "dynamodb.table-name")
@ConfigurationProperties("dynamodb")
interface DynamoConfiguration {
    @get:NotBlank
    val tableName: String
}
