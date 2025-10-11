package com.lvlup.backend.dto

import com.lvlup.backend.model.Book
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class CartResponse(
    val id: Long,
    val items: List<CartItemResponse>,
    val totalItems: Int,
    val totalAmount: BigDecimal
)

data class CartItemResponse(
    val id: Long,
    val book: Book,
    val quantity: Int,
    val availableStock: Int,
    val subtotal: BigDecimal
)

data class AddToCartRequest(
    @field:NotNull(message = "Book ID is required")
    @field:Positive(message = "Book ID must be positive")
    val bookId: Long,

    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @field:Max(value = 999, message = "Quantity must not exceed 999")
    val quantity: Int
)

data class UpdateCartItemRequest(
    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    @field:Max(value = 999, message = "Quantity must not exceed 999")
    val quantity: Int
)