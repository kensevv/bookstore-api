package com.lvlup.inventoryservice.exception.handler

import com.lvlup.inventoryservice.exception.EmptyCartException
import com.lvlup.inventoryservice.exception.InsufficientStockException
import com.lvlup.inventoryservice.exception.InvalidOperationException
import dto.ApiResponseFactory
import dto.ErrorResponse
import exception.BookstoreException
import exception.DuplicateResourceException
import exception.ResourceNotFoundException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(Exception::class)
    fun handleGlobalException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error occurred", ex)
        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            statusPhrase = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "An unexpected error occurred. Please try again later.",
            error = "Internal Server Error",
            path = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Validation error: ${ex.message}")
        val errors = ex.bindingResult.allErrors.associate { error ->
            val fieldName = (error as? FieldError)?.field ?: "unknown"
            val errorMessage = error.defaultMessage ?: "Invalid value"
            fieldName to errorMessage
        }

        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.BAD_REQUEST.value(),
            statusPhrase = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Some fields failed validation, check details for more information.",
            error = "Input validation failed",
            path = request.getDescription(false),
            details = errors
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicateResourceException(
        ex: DuplicateResourceException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Duplicate resource: ${ex.message}")
        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.CONFLICT.value(),
            statusPhrase = HttpStatus.CONFLICT.reasonPhrase,
            message = "Resource already exists",
            error = ex.message,
            path = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFoundException(
        ex: ResourceNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Resource not found: ${ex.message}")
        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.NOT_FOUND.value(),
            statusPhrase = HttpStatus.NOT_FOUND.reasonPhrase,
            message = "Resource not found",
            error = ex.message,
            path = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(InvalidOperationException::class, InsufficientStockException::class, EmptyCartException::class)
    fun handleBusinessLogicException(
        ex: BookstoreException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Business logic error: ${ex.message}")
        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.BAD_REQUEST.value(),
            statusPhrase = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Invalid operation",
            error = ex.message,
            path = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
}
