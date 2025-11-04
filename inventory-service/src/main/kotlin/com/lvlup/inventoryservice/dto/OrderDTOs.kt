package com.lvlup.inventoryservice.dto

import com.lvlup.inventoryservice.model.Book
import com.lvlup.inventoryservice.model.OrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateOrderRequest(
    @field:NotBlank(message = "Shipping address is required")
    @field:Size(min = 10, max = 500, message = "Shipping address must be between 10 and 500 characters")
    val shippingAddress: String
)

data class OrderResponse(
    val id: Long,
    val orderNumber: String,
    val userEmail: String,
    val items: List<OrderItemResponse>,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val shippingAddress: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class OrderItemResponse(
    val id: Long,
    val book: Book?,
    val quantity: Int,
    val priceAtPurchase: BigDecimal,
    val subtotal: BigDecimal
)