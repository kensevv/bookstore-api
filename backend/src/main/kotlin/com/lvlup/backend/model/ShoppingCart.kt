package com.lvlup.backend.model

import java.time.LocalDateTime

data class ShoppingCart(
    val id: Long?,
    val userEmail: String,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)

data class ShoppingCartItem(
    val id: Long?,
    val cartId: Long,
    val bookId: Long,
    val quantity: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)