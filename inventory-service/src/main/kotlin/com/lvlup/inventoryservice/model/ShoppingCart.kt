package com.lvlup.inventoryservice.model

import com.lvlup.inventoryservice.exception.InvalidOperationException
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDateTime

@Entity
@Table(
    name = "shopping_carts",
    indexes = [
        Index(name = "idx_carts_user_email", columnList = "user_email")
    ]
)
data class ShoppingCart(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_email", nullable = false, unique = true, length = 255)
    val userEmail: String,

    @OneToMany(
        mappedBy = "cart",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val items: MutableList<ShoppingCartItem> = mutableListOf(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun addItem(shoppingCart: ShoppingCart, book: Book, quantity: Int) {
        if (shoppingCart.id != this.id) {
            throw InvalidOperationException("Shopping cart miss match")
        }
        val existingItem = items.find { item -> item.bookId == book.id }
        if (existingItem != null) {
            existingItem.quantity += quantity
        } else {
            items.add(
                ShoppingCartItem(cart = shoppingCart, book = book, quantity = quantity)
            )
        }
    }

    fun removeItem(itemId: Long): Boolean {
        return items.removeIf { item -> item.id == itemId }
    }

    fun clearItems() {
        items.clear()
    }
}

@Entity
@Table(
    name = "shopping_carts_items",
    indexes = [
        Index(name = "idx_cart_items_cart", columnList = "cart_id"),
        Index(name = "idx_cart_items_book", columnList = "book_id")
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_cart_items_cart_book",
            columnNames = ["cart_id", "book_id"]
        )
    ]
)
data class ShoppingCartItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "cart_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_cart_items_cart")
    )
    val cart: ShoppingCart,

    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(
        name = "book_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_cart_items_book")
    )
    @NotFound(action = NotFoundAction.IGNORE)
    val book: Book? = null,

    @Column(name = "book_id", nullable = false, insertable = false, updatable = false)
    val bookId: Long = book?.id ?: 0,

    @Column(name = "quantity", nullable = false)
    var quantity: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)