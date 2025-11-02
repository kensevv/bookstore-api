package com.lvlup.commonspring.exception


// Base exception
open class BookstoreException(message: String) : RuntimeException(message)

// Resource not found exceptions
open class ResourceNotFoundException(message: String) : BookstoreException(message)

// Business logic exceptions
open class DuplicateResourceException(message: String) : BookstoreException(message)

class InvalidTokenException(message: String = "Invalid or expired token") : BookstoreException(message)