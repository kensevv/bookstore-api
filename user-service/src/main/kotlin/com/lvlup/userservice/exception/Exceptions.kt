package com.lvlup.userservice.exception

import exception.BookstoreException
import exception.DuplicateResourceException
import exception.ResourceNotFoundException

// Authentication exceptions
class InvalidCredentialsException(message: String = "Invalid email or password") : BookstoreException(message)
class UserAlreadyExistsException(message: String = "User with this email already exists") : DuplicateResourceException(message)
class UserNotFoundException(message: String = "User not found") : ResourceNotFoundException(message)
class UnauthorizedAccessException(message: String = "Unauthorized access") : BookstoreException(message)