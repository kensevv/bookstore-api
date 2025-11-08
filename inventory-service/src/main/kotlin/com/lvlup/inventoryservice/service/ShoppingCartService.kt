package com.lvlup.inventoryservice.service

import com.lvlup.inventoryservice.dto.AddToCartRequest
import com.lvlup.inventoryservice.dto.CartItemResponse
import com.lvlup.inventoryservice.dto.CartResponse
import com.lvlup.inventoryservice.dto.UpdateCartItemRequest
import com.lvlup.inventoryservice.exception.BookNotFoundException
import com.lvlup.inventoryservice.exception.CartNotFoundException
import com.lvlup.inventoryservice.exception.InsufficientStockException
import com.lvlup.inventoryservice.exception.UnauthorizedAccessException
import com.lvlup.inventoryservice.model.Book
import com.lvlup.inventoryservice.model.ShoppingCart
import com.lvlup.inventoryservice.model.ShoppingCartItem
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.ShoppingCartItemRepository
import com.lvlup.inventoryservice.repository.ShoppingCartRepository
import exception.ResourceNotFoundException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ShoppingCartService(
    private val cartRepository: ShoppingCartRepository,
    private val cartItemRepository: ShoppingCartItemRepository,
    private val bookRepository: BooksRepository
) {

    private val logger = KotlinLogging.logger {}

    private fun getOrCreateUsersShoppingCart(userEmail: String): ShoppingCart {
        logger.debug("Getting shopping-cart for user $userEmail or creating new one if absent.")
        return cartRepository.findByUserEmail(userEmail)
            ?: cartRepository.save(ShoppingCart(userEmail = userEmail))
    }


    @Transactional
    fun getUserCartAndRemoveInvalidItems(userEmail: String): CartResponse {
        val shoppingCart = getOrCreateUsersShoppingCart(userEmail)
        return shoppingCart.mapToCartResponseAndRemoveInvalidItems()
    }

    private fun ShoppingCart.mapToCartResponseAndRemoveInvalidItems(): CartResponse {

        this.items.filter { it.book == null }.forEach { deletedBookItem ->
            logger.warn("Book ${deletedBookItem.bookId} not found, removing from cart ${this.id}, cart Item ID: ${deletedBookItem.id}")
            this.removeItem(deletedBookItem.id!!)
        }

        val cartItemsResponses = this.items.map { item ->
            CartItemResponse(
                id = item.id!!,
                book = item.book!!,
                quantity = item.quantity,
                availableStock = item.book.stock,
                subtotal = item.book.price * item.quantity.toBigDecimal()
            )
        }

        return CartResponse(
            id = this.id!!,
            items = cartItemsResponses,
            totalItems = cartItemsResponses.sumOf { it.quantity },
            totalAmount = cartItemsResponses.sumOf { it.subtotal }
        )
    }

    @Transactional
    fun addItemToUserCart(userEmail: String, request: AddToCartRequest): CartResponse {
        logger.info("Adding book ${request.bookId}, quantity: ${request.quantity} to cart for user $userEmail")

        val book = bookRepository.findById(request.bookId).orElseThrow {
            BookNotFoundException("Book not found with ID: ${request.bookId}. Can't add to cart.")
        }

        if (book.stock < request.quantity) {
            throw InsufficientStockException(
                "Insufficient stock for book '${book.title}'. Available: ${book.stock}, Requested: ${request.quantity}"
            )
        }

        val shoppingCart = getOrCreateUsersShoppingCart(userEmail)

        val existingItem = cartItemRepository.findByCartIdAndBookId(shoppingCart.id!!, request.bookId)

        if (existingItem != null) {
            updateExistingCartItem(existingItem, book, request.quantity)
        } else {
            addNewCartItem(shoppingCart, book, request.quantity)
        }

        return shoppingCart.mapToCartResponseAndRemoveInvalidItems()
    }

    private fun updateExistingCartItem(existingItem: ShoppingCartItem, book: Book, addedQuantity: Int) {
        val newQuantity = existingItem.quantity + addedQuantity
        if (book.stock < newQuantity) {
            throw InsufficientStockException(
                "Insufficient stock for book '${book.title}'. Available: ${book.stock}, " +
                        "Current in cart: ${existingItem.quantity}, Requested to add: $addedQuantity"
            )
        }

        existingItem.quantity = newQuantity
        cartItemRepository.save(existingItem)
        logger.info("Updated cart item quantity to $newQuantity")
    }

    private fun addNewCartItem(cart: ShoppingCart, book: Book, quantity: Int) {
        val createdAt = LocalDateTime.now()
        val cartItem = ShoppingCartItem(
            cart = cart,
            book = book,
            quantity = quantity,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
        cartItemRepository.save(cartItem)
        logger.info("Added new item to cart")
    }

    @Transactional
    fun updateCartItemQuantity(userEmail: String, cartItemId: Long, request: UpdateCartItemRequest): CartResponse {
        logger.info("Updating cart item $cartItemId for user $userEmail with quantity ${request.quantity}")

        val currentUserShoppingCart = cartRepository.findByUserEmail(userEmail)
            ?: throw CartNotFoundException()

        val cartItemToUpdate = currentUserShoppingCart.items.find { it.id == cartItemId }
            ?: throw ResourceNotFoundException("Cart item not found with ID: $cartItemId for user $userEmail")


        // check if item belongs to user's cart
        if (cartItemToUpdate.cart.id != currentUserShoppingCart.id) {
            throw UnauthorizedAccessException("You don't have permission to modify this cart item")
        }

        if (cartItemToUpdate.book == null) {
            throw ResourceNotFoundException("Book not found with ID: ${cartItemToUpdate.bookId}")
        }

        updateExistingCartItem(cartItemToUpdate, cartItemToUpdate.book, request.quantity)

        return currentUserShoppingCart.mapToCartResponseAndRemoveInvalidItems()
    }

    @Transactional
    fun removeItemFromUserCart(userEmail: String, itemId: Long): CartResponse {
        logger.info("Removing item $itemId from cart for user $userEmail")

        val cart = cartRepository.findByUserEmail(userEmail)
            ?: throw CartNotFoundException()

        val removed = cart.removeItem(itemId)
        if (!removed) throw ResourceNotFoundException("Cart item not found with ID: $itemId for user $userEmail")

        cartItemRepository.deleteById(itemId)
        logger.info("Item removed from cart successfully")

        return cart.mapToCartResponseAndRemoveInvalidItems()
    }
}