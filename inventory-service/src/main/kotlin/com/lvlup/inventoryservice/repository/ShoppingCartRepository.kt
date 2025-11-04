package com.lvlup.inventoryservice.repository

import com.lvlup.inventoryservice.model.Book
import com.lvlup.inventoryservice.model.Category
import com.lvlup.inventoryservice.model.ShoppingCart
import com.lvlup.inventoryservice.model.ShoppingCartItem
import com.lvlup.bookstore.jooq.tables.Books.Companion.BOOKS
import com.lvlup.bookstore.jooq.tables.ShoppingCarts.Companion.SHOPPING_CARTS
import com.lvlup.bookstore.jooq.tables.pojos.Books
import com.lvlup.bookstore.jooq.tables.records.BooksRecord
import com.lvlup.bookstore.jooq.tables.records.CategoriesRecord
import com.lvlup.bookstore.jooq.tables.records.ShoppingCartsItemsRecord
import com.lvlup.bookstore.jooq.tables.records.ShoppingCartsRecord
import com.lvlup.bookstore.jooq.tables.references.CATEGORIES
import com.lvlup.bookstore.jooq.tables.references.SHOPPING_CARTS_ITEMS
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.Long
import kotlin.String

@Repository
class ShoppingCartRepository(private val dsl: DSLContext) {

    fun findCartByUserEmail(userEmail: String): ShoppingCart? {
        return dsl.selectFrom(SHOPPING_CARTS)
            .where(SHOPPING_CARTS.USER_EMAIL.eq(userEmail))
            .fetchOne()?.mapToCart()
    }

    fun findAllCartItemsAndTheirBooks(cartId: Long): List<Pair<ShoppingCartItem, Book?>> {
        return dsl.select(
            SHOPPING_CARTS_ITEMS.asterisk(),
            BOOKS.asterisk(),
            CATEGORIES.asterisk()
        ).from(SHOPPING_CARTS_ITEMS)
            .leftJoin(BOOKS)
            .on(SHOPPING_CARTS_ITEMS.BOOK_ID.eq(BOOKS.ID))
            .and(BOOKS.DELETED.isFalse)
            .leftJoin(CATEGORIES)
            .on(BOOKS.CATEGORY_ID.eq(CATEGORIES.ID))
            .where(SHOPPING_CARTS_ITEMS.CART_ID.eq(cartId))
            .fetch()
            .map {
                mapCartItemToBookPair(it)
            }
    }

    @Transactional
    fun createNewShoppingCartForUser(userEmail: String): ShoppingCart {
        val createdAt = LocalDateTime.now()

        return dsl.newRecord(SHOPPING_CARTS).apply {
            this.userEmail = userEmail
            this.createdAt = createdAt
            this.updatedAt = createdAt
        }.let { newCart ->
            newCart.store()
            newCart.mapToCart()
        }
    }

    fun findCartItemById(itemId: Long): ShoppingCartItem? {
        return dsl.selectFrom(SHOPPING_CARTS_ITEMS)
            .where(SHOPPING_CARTS_ITEMS.ID.eq(itemId))
            .fetchOne()?.mapToCartItem()
    }

    fun findCartItemByBookId(cartId: Long, bookId: Long): ShoppingCartItem? {
        return dsl.selectFrom(SHOPPING_CARTS_ITEMS)
            .where(SHOPPING_CARTS_ITEMS.CART_ID.eq(cartId))
            .and(SHOPPING_CARTS_ITEMS.BOOK_ID.eq(bookId))
            .fetchOne()?.mapToCartItem()
    }


    @Transactional
    fun addNewCartItem(cartItem: ShoppingCartItem): ShoppingCartItem {
        return dsl.newRecord(SHOPPING_CARTS_ITEMS).apply {
            this.cartId = cartItem.cartId
            this.bookId = cartItem.bookId
            this.quantity = cartItem.quantity
            this.createdAt = cartItem.createdAt
            this.updatedAt = cartItem.updatedAt
        }.let { newCartItem ->
            newCartItem.store()
            newCartItem.mapToCartItem()
        }
    }

    @Transactional
    fun updateCartItemQuantity(itemId: Long, quantity: Int) {
        dsl.update(SHOPPING_CARTS_ITEMS)
            .set(SHOPPING_CARTS_ITEMS.QUANTITY, quantity)
            .set(SHOPPING_CARTS_ITEMS.UPDATED_AT, LocalDateTime.now())
            .where(SHOPPING_CARTS_ITEMS.ID.eq(itemId))
            .execute()
    }

    @Transactional
    fun deleteCartItem(itemId: Long) {
        dsl.deleteFrom(SHOPPING_CARTS_ITEMS)
            .where(SHOPPING_CARTS_ITEMS.ID.eq(itemId))
            .execute()
    }

    @Transactional
    fun clearCart(cartId: Long) {
        dsl.deleteFrom(SHOPPING_CARTS_ITEMS)
            .where(SHOPPING_CARTS_ITEMS.CART_ID.eq(cartId))
            .execute()
    }

    private fun ShoppingCartsRecord.mapToCart(): ShoppingCart {
        return ShoppingCart(
            id = id,
            userEmail = userEmail!!,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ShoppingCartsItemsRecord.mapToCartItem(): ShoppingCartItem {
        return ShoppingCartItem(
            id = id,
            cartId = cartId!!,
            bookId = bookId!!,
            quantity = quantity!!,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun mapCartItemToBookPair(
        resultRecord: Record
    ): Pair<ShoppingCartItem, Book?> {
        val extractedCartItem =
            resultRecord.into(ShoppingCartsItemsRecord::class.java).mapToCartItem()

        val extractedBookRecord =
            resultRecord.into(BooksRecord::class.java).nullIfPrimaryKeyIsNull()?.into(Books::class.java)
                ?: return extractedCartItem to null // left join matched no books for cart item (primary key column null)

        val extractedCategoryRecord = resultRecord.into(CategoriesRecord::class.java)

        return extractedCartItem to Book(
            id = extractedBookRecord.id,
            title = extractedBookRecord.title!!,
            author = extractedBookRecord.author!!,
            description = extractedBookRecord.description,
            price = extractedBookRecord.price!!,
            stock = extractedBookRecord.stock!!,
            category = Category(
                id = extractedCategoryRecord.id,
                name = extractedCategoryRecord.name!!,
                description = extractedCategoryRecord.description,
                createdAt = extractedCategoryRecord.createdAt!!,
                updatedAt = extractedCategoryRecord.updatedAt!!,
            ),
            coverImageUrl = extractedBookRecord.coverImageUrl,
            createdAt = extractedBookRecord.createdAt!!,
            updatedAt = extractedBookRecord.updatedAt!!,
            deleted = extractedBookRecord.deleted!!,
        )
    }

}