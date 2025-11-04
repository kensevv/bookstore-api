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
import com.lvlup.inventoryservice.repository.ShoppingCartRepository
import exception.ResourceNotFoundException
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.collections.mapNotNull

@Service
class ShoppingCartService(
    private val cartRepository: ShoppingCartRepository,
    private val bookRepository: BooksRepository
) {

    private val logger = KotlinLogging.logger {}

    private fun getOrCreateUsersShoppingCart(userEmail: String): ShoppingCart =
        cartRepository.findCartByUserEmail(userEmail)
            ?: cartRepository.createNewShoppingCartForUser(userEmail)

    @Transactional
    fun getUserCartAndRemoveInvalidItems(userEmail: String): CartResponse {
        logger.debug("Fetching shopping-cart for user $userEmail")

        val shoppingCart = getOrCreateUsersShoppingCart(userEmail)

        val cartItems = cartRepository.findAllCartItemsAndTheirBooks(shoppingCart.id!!)

        val cartItemsResponses = cartItems.mapNotNull { (item, book) ->
            if (book == null) {
                logger.warn("Book ${item.bookId} not found, removing from cart ${shoppingCart.id}")
                cartRepository.deleteCartItem(item.id!!)
                null
            } else {
                CartItemResponse(
                    id = item.id!!,
                    book = book,
                    quantity = item.quantity,
                    availableStock = book.stock,
                    subtotal = book.price * item.quantity.toBigDecimal()
                )
            }
        }

        return CartResponse(
            id = shoppingCart.id,
            items = cartItemsResponses,
            totalItems = cartItemsResponses.sumOf { it.quantity },
            totalAmount = cartItemsResponses.sumOf { it.subtotal }
        )
    }

    @Transactional
    fun addItemToUserCart(userEmail: String, request: AddToCartRequest): CartResponse {
        logger.info("Adding book ${request.bookId} to cart for user $userEmail")

        val book = bookRepository.findBookById(request.bookId)
            ?: throw BookNotFoundException("Book not found with ID: ${request.bookId}. Can't add to cart.")

        validateStockAvailability(book, request.quantity)

        val shoppingCart = getOrCreateUsersShoppingCart(userEmail)
        val existingItem = cartRepository.findCartItemByBookId(shoppingCart.id!!, request.bookId)

        if (existingItem != null) {
            updateExistingCartItem(existingItem, book, request.quantity)
        } else {
            addNewCartItem(shoppingCart.id, request)
        }

        return getUserCartAndRemoveInvalidItems(userEmail)
    }

    private fun validateStockAvailability(book: Book, requestedQuantity: Int) {
        if (book.stock < requestedQuantity) {
            throw InsufficientStockException(
                "Insufficient stock for book '${book.title}'. Available: ${book.stock}, Requested: $requestedQuantity"
            )
        }
    }

    private fun updateExistingCartItem(existingItem: ShoppingCartItem, book: Book, addedQuantity: Int) {
        val newQuantity = existingItem.quantity + addedQuantity
        if (book.stock < newQuantity) {
            throw InsufficientStockException(
                "Insufficient stock for book '${book.title}'. Available: ${book.stock}, " +
                        "Current in cart: ${existingItem.quantity}, Requested to add: $addedQuantity"
            )
        }

        cartRepository.updateCartItemQuantity(existingItem.id!!, newQuantity)
        logger.info("Updated cart item quantity to $newQuantity")
    }

    private fun addNewCartItem(cartId: Long, request: AddToCartRequest) {
        val createdAt = LocalDateTime.now()
        val cartItem = ShoppingCartItem(
            id = null,
            cartId = cartId,
            bookId = request.bookId,
            quantity = request.quantity,
            createdAt = createdAt,
            updatedAt = createdAt
        )
        cartRepository.addNewCartItem(cartItem)
        logger.info("Added new item to cart")
    }

    @Transactional
    fun updateCartItemQuantity(userEmail: String, cartItemId: Long, request: UpdateCartItemRequest): CartResponse {
        logger.info("Updating cart item $cartItemId for user $userEmail with quantity ${request.quantity}")

        val currentUserShoppingCart = cartRepository.findCartByUserEmail(userEmail)
            ?: throw CartNotFoundException()

        val cartItemToUpdate = cartRepository.findCartItemById(cartItemId)
            ?: throw ResourceNotFoundException("Cart item not found with ID: $cartItemId")

        // check if item belongs to user's cart
        if (cartItemToUpdate.cartId != currentUserShoppingCart.id) {
            throw UnauthorizedAccessException("You don't have permission to modify this cart item")
        }

        // Validate stock
        val book = bookRepository.findBookById(cartItemToUpdate.bookId)
            ?: throw BookNotFoundException("Book not found")

        if (book.stock < request.quantity) {
            throw InsufficientStockException(
                "Insufficient stock for book '${book.title}'. Available: ${book.stock}, Requested: ${request.quantity}"
            )
        }

        cartRepository.updateCartItemQuantity(cartItemId, request.quantity)
        logger.info("Cart item updated successfully")

        return getUserCartAndRemoveInvalidItems(userEmail)
    }

    @Transactional
    fun removeItemFromUserCart(userEmail: String, itemId: Long): CartResponse {
        logger.info("Removing item $itemId from cart for user $userEmail")

        val cart = cartRepository.findCartByUserEmail(userEmail)
            ?: throw CartNotFoundException()

        val cartItem = cartRepository.findCartItemById(itemId)
            ?: throw ResourceNotFoundException("Cart item not found with ID: $itemId")

        // check if item belongs to user's cart
        if (cartItem.cartId != cart.id) {
            throw UnauthorizedAccessException("You don't have permission to modify this cart item")
        }

        cartRepository.deleteCartItem(itemId)
        logger.info("Item removed from cart successfully")

        return getUserCartAndRemoveInvalidItems(userEmail)
    }
}