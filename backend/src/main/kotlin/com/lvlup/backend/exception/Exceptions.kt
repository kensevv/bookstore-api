package com.lvlup.backend.exception

import jakarta.security.auth.message.AuthException
import org.springframework.http.HttpStatus

// Base exception
sealed class BookstoreException(message: String) : RuntimeException(message)

// Authentication exceptions
class InvalidCredentialsException(message: String = "Invalid email or password") : BookstoreException(message)
class UserAlreadyExistsException(message: String = "User with this email already exists") : BookstoreException(message)
class InvalidTokenException(message: String = "Invalid or expired token") : BookstoreException(message)
class UnauthorizedAccessException(message: String = "Unauthorized access") : BookstoreException(message)

// Resource not found exceptions
open class ResourceNotFoundException(message: String) : BookstoreException(message)
class UserNotFoundException(message: String = "User not found") : ResourceNotFoundException(message)