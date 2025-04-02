package top.sunbath.api.shared.response

import io.micronaut.serde.annotation.Serdeable

/**
 * API error details.
 * @property code The error code
 * @property message The error message
 * @property details Additional error details (optional)
 */
@Serdeable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null,
) {
    companion object {
        // Common error codes
        const val VALIDATION_ERROR = "VALIDATION_ERROR"
        const val NOT_FOUND = "NOT_FOUND"
        const val UNAUTHORIZED = "UNAUTHORIZED"
        const val FORBIDDEN = "FORBIDDEN"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
        const val EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED"

        /**
         * Create a validation error.
         * @param message The error message
         * @param details The validation error details
         */
        fun validationError(
            message: String,
            details: Map<String, Any>? = null,
        ): ApiError =
            ApiError(
                code = VALIDATION_ERROR,
                message = message,
                details = details,
            )

        /**
         * Create a not found error.
         * @param message The error message
         */
        fun notFound(message: String): ApiError =
            ApiError(
                code = NOT_FOUND,
                message = message,
            )

        /**
         * Create an unauthorized error.
         * @param message The error message
         */
        fun unauthorized(message: String): ApiError =
            ApiError(
                code = UNAUTHORIZED,
                message = message,
            )

        /**
         * Create a forbidden error.
         * @param message The error message
         */
        fun forbidden(message: String): ApiError =
            ApiError(
                code = FORBIDDEN,
                message = message,
            )

        /**
         * Create an internal error.
         * @param message The error message
         */
        fun internalError(message: String): ApiError =
            ApiError(
                code = INTERNAL_ERROR,
                message = message,
            )

        /**
         * Create an email not verified error.
         * @param message The error message
         */
        fun emailNotVerified(message: String): ApiError =
            ApiError(
                code = EMAIL_NOT_VERIFIED,
                message = message,
            )
    }
}
