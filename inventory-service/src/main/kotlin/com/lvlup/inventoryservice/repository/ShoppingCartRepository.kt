package com.lvlup.inventoryservice.repository

import com.lvlup.inventoryservice.model.ShoppingCart
import com.lvlup.inventoryservice.model.ShoppingCartItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import kotlin.Long
import kotlin.String

interface ShoppingCartRepository : JpaRepository<ShoppingCart, Long> {
    fun findByUserEmail(userEmail: String): ShoppingCart?
}

interface ShoppingCartItemRepository : JpaRepository<ShoppingCartItem, Long> {

    @Query(
        """
        SELECT i FROM ShoppingCartItem i
        WHERE i.cart.id = :cartId AND i.book.id = :bookId
    """
    )
    fun findByCartIdAndBookId(cartId: Long, bookId: Long): ShoppingCartItem?

}