package top.sunbath.shared.types

data class SqsMessage<T>(
    val id: String,
    val data: T,
)
