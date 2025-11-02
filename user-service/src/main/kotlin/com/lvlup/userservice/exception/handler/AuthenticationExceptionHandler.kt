package com.lvlup.userservice.exception.handler

import com.lvlup.commonspring.exception.InvalidTokenException
import com.lvlup.userservice.exception.InvalidCredentialsException
import com.lvlup.userservice.exception.UnauthorizedAccessException
import com.lvlup.userservice.exception.UserAlreadyExistsException
import dto.ApiResponseFactory
import dto.ErrorResponse
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest

@RestControllerAdvice
class AuthenticationExceptionHandler {
    private val logger = KotlinLogging.logger {}

    @ExceptionHandler(InvalidCredentialsException::class, BadCredentialsException::class)
    fun handleInvalidCredentialsException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Invalid credentials attempt")

        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.UNAUTHORIZED.value(),
            statusPhrase = HttpStatus.UNAUTHORIZED.reasonPhrase,
            message = "Invalid email or password",
            error = ex.message,
            path = request.getDescription(false)
        )

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(UnauthorizedAccessException::class, AccessDeniedException::class, InvalidTokenException::class)
    fun handleUnauthorizedException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Unauthorized access attempt: ${ex.message}")
        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.FORBIDDEN.value(),
            statusPhrase = HttpStatus.FORBIDDEN.reasonPhrase,
            message = "You don't have permission to access this resource",
            error = ex.message,
            path = request.getDescription(false)
        )

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(UserAlreadyExistsException::class)
    fun handleUserAlreadyExistsException(
        ex: UserAlreadyExistsException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        logger.warn("Duplicate resource: ${ex.message}")
        val errorResponse = ApiResponseFactory.error(
            statusCode = HttpStatus.CONFLICT.value(),
            statusPhrase = HttpStatus.CONFLICT.reasonPhrase,
            message = "User with this email already exists",
            error = ex.message,
            path = request.getDescription(false)
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

}