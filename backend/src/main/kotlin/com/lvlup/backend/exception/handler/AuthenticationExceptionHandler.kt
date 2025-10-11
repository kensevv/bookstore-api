package com.lvlup.backend.exception.handler

import com.lvlup.backend.dto.ApiResponseFactory
import com.lvlup.backend.dto.ErrorResponse
import com.lvlup.backend.exception.InvalidCredentialsException
import com.lvlup.backend.exception.InvalidTokenException
import com.lvlup.backend.exception.UnauthorizedAccessException
import com.lvlup.backend.exception.UserAlreadyExistsException
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
            status = HttpStatus.UNAUTHORIZED,
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
            status = HttpStatus.FORBIDDEN,
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
            status = HttpStatus.CONFLICT,
            message = "User with this email already exists",
            error = ex.message,
            path = request.getDescription(false)
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

}