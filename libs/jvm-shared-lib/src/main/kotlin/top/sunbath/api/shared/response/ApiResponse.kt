package top.sunbath.api.shared.response

import io.micronaut.serde.annotation.Serdeable

/**
 * Generic API response wrapper.
 * @param T The type of data in the response
 * @property success Whether the request was successful
 * @property data The response data
 * @property error The error details if the request failed
 */
@Serdeable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ApiError? = null,
) {
    companion object {
        /**
         * Create a successful response with data.
         * @param data The response data
         */
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(
                success = true,
                data = data,
            )

        /**
         * Create a failed response with error.
         * @param error The error details
         */
        fun <T> error(error: ApiError): ApiResponse<T> =
            ApiResponse(
                success = false,
                error = error,
            )
    }
}
