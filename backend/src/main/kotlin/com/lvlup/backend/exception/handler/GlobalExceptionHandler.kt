package com.lvlup.backend.exception.handler

import com.lvlup.backend.dto.ApiResponseFactory
import com.lvlup.backend.dto.ErrorResponse
import com.lvlup.backend.exception.BookstoreException
import com.lvlup.backend.exception.DuplicateResourceException
import com.lvlup.backend.exception.EmptyCartException
import com.lvlup.backend.exception.InsufficientStockException
import com.lvlup.backend.exception.InvalidOperationException
import com.lvlup.backend.exception.ResourceNotFoundException
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
            status = HttpStatus.INTERNAL_SERVER_ERROR,
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
            status = HttpStatus.BAD_REQUEST,
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
            status = HttpStatus.CONFLICT,
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
            status = HttpStatus.NOT_FOUND,
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
            status = HttpStatus.BAD_REQUEST,
            message = "Invalid operation",
            error = ex.message,
            path = request.getDescription(false)
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }
}
