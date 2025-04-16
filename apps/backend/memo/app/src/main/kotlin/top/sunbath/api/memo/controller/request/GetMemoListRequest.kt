package top.sunbath.api.memo.controller.request

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.serde.annotation.Serdeable
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import top.sunbath.api.memo.repository.MemoSort

@Introspected
@Serdeable
data class GetMemoListRequestFilter(
    @field:Nullable
    val isCompleted: Boolean?,
    @field:Nullable
    val isDeleted: Boolean?,
)

typealias GetMemoListRequestSort = MemoSort

@Introspected
@Serdeable
data class GetMemoListRequest(
    @field:Nullable
    val filter: GetMemoListRequestFilter?,
    @field:Nullable
    val sort: GetMemoListRequestSort?,
    @field:NotNull
    @field:Max(100)
    @field:Min(1)
    val limit: Int,
    @field:Nullable
    val cursor: String?,
)
