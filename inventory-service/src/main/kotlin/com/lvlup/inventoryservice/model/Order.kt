package com.lvlup.inventoryservice.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
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

@Entity
@Table(
    name = "orders",
    indexes = [
        Index(name = "idx_orders_user", columnList = "user_email"),
        Index(name = "idx_orders_status", columnList = "status")
    ]
)
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_email", nullable = false)
    val userEmail: String,

    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    val orderNumber: String,

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    val totalAmount: BigDecimal,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus,

    @Column(name = "shipping_address", nullable = false, columnDefinition = "TEXT")
    val shippingAddress: String,

    @OneToMany(
        mappedBy = "order",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val items: MutableList<OrderItem> = mutableListOf(),

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime? = null
) {
    fun addOrderItem(
        book: Book,
        quantity: Int,
        priceAtPurchase: BigDecimal,
        createdAt: LocalDateTime? = null
    ) {
        val item = OrderItem(
            order = this,
            book = book,
            quantity = quantity,
            priceAtPurchase = priceAtPurchase,
            createdAt = createdAt,
        )
        items.add(item)
    }
}

@Entity
@Table(
    name = "order_items",
    indexes = [
        Index(name = "idx_order_items_order", columnList = "order_id")
    ]
)
data class OrderItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    val order: Order,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    val book: Book,

    @Column(nullable = false)
    val quantity: Int,

    @Column(name = "price_at_purchase", nullable = false, precision = 10, scale = 2)
    val priceAtPurchase: BigDecimal,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)