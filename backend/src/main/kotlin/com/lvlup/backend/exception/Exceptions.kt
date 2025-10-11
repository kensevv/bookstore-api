package com.lvlup.backend.exception


// Base exception
sealed class BookstoreException(message: String) : RuntimeException(message)

// Resource not found exceptions
open class ResourceNotFoundException(message: String) : BookstoreException(message)
class UserNotFoundException(message: String = "User not found") : ResourceNotFoundException(message)
class BookNotFoundException(message: String = "Book not found") : ResourceNotFoundException(message)
class CategoryNotFoundException(message: String = "Category not found") : ResourceNotFoundException(message)
class OrderNotFoundException(message: String = "Order not found") : ResourceNotFoundException(message)
class CartNotFoundException(message: String = "Shopping cart not found") : ResourceNotFoundException(message)

// Business logic exceptions
open class DuplicateResourceException(message: String) : BookstoreException(message)
class InsufficientStockException(message: String = "Insufficient stock available") : BookstoreException(message)
class InvalidOperationException(message: String) : BookstoreException(message)
class EmptyCartException(message: String = "Cart is empty") : BookstoreException(message)
class UnauthorizedAccessException(message: String = "Unauthorized access") : BookstoreException(message)

// Authentication exceptions
class InvalidCredentialsException(message: String = "Invalid email or password") : BookstoreException(message)
class UserAlreadyExistsException(message: String = "User with this email already exists") : DuplicateResourceException(message)
class InvalidTokenException(message: String = "Invalid or expired token") : BookstoreException(message)