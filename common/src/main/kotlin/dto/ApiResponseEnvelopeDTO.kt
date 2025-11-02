package dto

import java.time.LocalDateTime

// Following JSend specifications https://github.com/omniti-labs/jsend
interface ApiResponse {
    val timestamp: LocalDateTime
    val status: String
    val message: String?
}

data class ErrorResponse(
    override val timestamp: LocalDateTime,
    override val status: String,
    override val message: String,
    val error: String,
    val code: Int,
    val path: String,
    val details: Map<String, Any>?,
) : ApiResponse

data class SuccessResponse<T>(
    override val timestamp: LocalDateTime,
    override val status: String,
    override val message: String? = null,
    val data: T? = null,
) : ApiResponse

object ApiResponseFactory {
    fun <T> success(
        data: T? = null,
        message: String? = null,
    ) = SuccessResponse(
        timestamp = LocalDateTime.now(),
        status = "success",
        message = message,
        data = data
    )

    fun error(
        statusCode: Int,
        statusPhrase: String,
        message: String,
        error: String?,
        path: String,
        details: Map<String, Any>? = null
    ) = ErrorResponse(
        timestamp = LocalDateTime.now(),
        status = "error",
        code = statusCode,
        message = message,
        error = error ?: statusPhrase,
        path = path.removePrefix("uri="),
        details = details
    )
}
