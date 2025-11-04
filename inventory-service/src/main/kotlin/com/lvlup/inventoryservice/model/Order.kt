package com.lvlup.inventoryservice.model

import java.math.BigDecimal
import java.time.LocalDateTime


enum class OrderStatus {
    PENDING, SHIPPED, DELIVERED, CANCELLED;

    companion object {
        fun getValidTransitions(status: OrderStatus): Set<OrderStatus> {
            return when (status) {
                PENDING -> setOf(SHIPPED, CANCELLED)
                SHIPPED -> setOf(DELIVERED, CANCELLED)
                DELIVERED -> emptySet()
                CANCELLED -> emptySet()
            }
        }
    }
}

data class Order(
    val id: Long?,
    val userEmail: String,
    val orderNumber: String,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val shippingAddress: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class OrderItem(
    val id: Long?,
    val orderId: Long,
    val bookId: Long,
    val quantity: Int,
    val priceAtPurchase: BigDecimal,
    val createdAt: LocalDateTime
)