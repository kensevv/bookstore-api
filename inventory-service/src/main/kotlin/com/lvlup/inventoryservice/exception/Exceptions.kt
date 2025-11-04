package com.lvlup.inventoryservice.exception

import exception.BookstoreException
import exception.ResourceNotFoundException

class BookNotFoundException(message: String = "Book not found") : ResourceNotFoundException(message)
class CategoryNotFoundException(message: String = "Category not found") : ResourceNotFoundException(message)
class OrderNotFoundException(message: String = "Order not found") : ResourceNotFoundException(message)
class CartNotFoundException(message: String = "Shopping cart not found") : ResourceNotFoundException(message)

class InsufficientStockException(message: String = "Insufficient stock available") : BookstoreException(message)
class InvalidOperationException(message: String) : BookstoreException(message)
class EmptyCartException(message: String = "Cart is empty") : BookstoreException(message)

class UnauthorizedAccessException(message: String = "Unauthorized access") : BookstoreException(message)
