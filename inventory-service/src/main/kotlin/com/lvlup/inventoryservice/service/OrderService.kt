package com.lvlup.inventoryservice.service

import com.lvlup.inventoryservice.dto.CreateOrderRequest
import com.lvlup.inventoryservice.dto.OrderItemResponse
import com.lvlup.inventoryservice.dto.OrderResponse
import com.lvlup.inventoryservice.exception.BookNotFoundException
import com.lvlup.inventoryservice.exception.EmptyCartException
import com.lvlup.inventoryservice.exception.InsufficientStockException
import com.lvlup.inventoryservice.exception.InvalidOperationException
import com.lvlup.inventoryservice.exception.OrderNotFoundException
import com.lvlup.inventoryservice.exception.UnauthorizedAccessException
import com.lvlup.inventoryservice.model.Book
import com.lvlup.inventoryservice.model.Order
import com.lvlup.inventoryservice.model.OrderItem
import com.lvlup.inventoryservice.model.OrderStatus
import com.lvlup.inventoryservice.model.ShoppingCart
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.OrderRepository
import com.lvlup.inventoryservice.repository.ShoppingCartRepository
import dto.PaginatedDataResponse
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.collections.map

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val cartRepository: ShoppingCartRepository,
    private val bookRepository: BooksRepository,
) {

    private val logger = KotlinLogging.logger {}

    @Transactional
    fun createUserOrder(userEmail: String, request: CreateOrderRequest): OrderResponse {
        logger.info("Creating shopping cart order for user $userEmail")

        val userShoppingCart = cartRepository.findCartByUserEmail(userEmail)
            ?: throw EmptyCartException()

        val shoppingCartOrderItemsData = validateCartAndGetItems(userShoppingCart)

        val createdAt = LocalDateTime.now()

        val savedOrder = createAndSaveOrder(userEmail, shoppingCartOrderItemsData, request.shippingAddress, createdAt)

        processOrderItemsAndHandleStock(shoppingCartOrderItemsData, savedOrder, createdAt)

        cartRepository.clearCart(userShoppingCart.id!!)

        logger.info("Order placement completed successfully: ${savedOrder.orderNumber}")

        return getUserOrderById(savedOrder.id!!, userEmail)
    }

    private fun processOrderItemsAndHandleStock(
        shoppingCartOrderItemsData: List<Triple<Long, Int, BigDecimal>>,
        savedOrder: Order,
        createdAt: LocalDateTime
    ) {
        shoppingCartOrderItemsData.forEach { (bookId, quantity, price) ->
            // Save order item
            orderRepository.saveOrderItem(
                OrderItem(
                    id = null,
                    orderId = savedOrder.id!!,
                    bookId = bookId,
                    quantity = quantity,
                    priceAtPurchase = price,
                    createdAt = createdAt
                )
            )

            // Deduct stock
            val stockDeducted = bookRepository.decrementStock(bookId, quantity) // conditional update
            if (!stockDeducted) {
                throw InsufficientStockException(
                    "Failed to deduct stock for book ID: $bookId. Concurrent stock issue."
                )
            }
        }
    }


    private fun validateCartAndGetItems(cart: ShoppingCart): List<Triple<Long, Int, BigDecimal>> {

        val cartItems = cartRepository.findAllCartItemsAndTheirBooks(cart.id!!)

        if (cartItems.isEmpty()) {
            throw EmptyCartException("Cannot create order from empty cart")
        }

        // Validate all items, check stock
        val orderItemsData = cartItems.map { (cartItem, book) ->
            if (book == null) {
                throw BookNotFoundException("Book with ID ${cartItem.bookId} not found")
            }

            if (book.stock < cartItem.quantity) {
                throw InsufficientStockException(
                    "Insufficient stock for book '${book.title}'. " +
                            "Available: ${book.stock}, Required: ${cartItem.quantity}"
                )
            }

            Triple(book.id!!, cartItem.quantity, book.price)
        }

        return orderItemsData
    }

    private fun createAndSaveOrder(
        userEmail: String,
        orderItemsData: List<Triple<Long, Int, BigDecimal>>,
        shippingAddress: String,
        createdAt: LocalDateTime
    ): Order {
        val newOrderNumber = generateNewOrderNumber()
        val order = Order(
            id = null,
            userEmail = userEmail,
            orderNumber = newOrderNumber,
            totalAmount = orderItemsData.sumOf { it.second.toBigDecimal() * it.third },
            status = OrderStatus.PENDING,
            shippingAddress = shippingAddress.trim(),
            createdAt = createdAt,
            updatedAt = createdAt
        )

        val savedOrder = orderRepository.createNewOrder(order)
        logger.info("Order created with order number: $newOrderNumber and id ${savedOrder.id}")

        return savedOrder
    }

    private fun generateNewOrderNumber(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "ORD-$timestamp-$random"
    }


    @Transactional(readOnly = true)
    fun getUserOrderById(orderId: Long, userEmail: String): OrderResponse {
        logger.debug("Fetching order $orderId for user $userEmail")

        val order = orderRepository.findOrderById(orderId)
            ?: throw OrderNotFoundException("Order not found with ID: $orderId")

        // Verify order belongs to user
        if (order.userEmail != userEmail) {
            throw UnauthorizedAccessException("You don't have permission to view this order")
        }

        return mapToOrderResponse(order)
    }

    @Transactional(readOnly = true)
    fun getOrdersPaginatedByUserEmail(
        userEmail: String,
        page: Int,
        size: Int,
        status: OrderStatus?
    ): PaginatedDataResponse<OrderResponse> {
        logger.debug("Fetching orders for user $userEmail - page: $page, size: $size")

        val validatedPage = maxOf(0, page)
        val validatedSize = minOf(maxOf(1, size), 100)

        val orders = orderRepository.findOrdersByUserEmail(userEmail, status, validatedPage, validatedSize)
        val totalElements = orderRepository.countOrdersByUserEmail(userEmail)
        val totalPages = if (totalElements == 0L) 0 else (totalElements + validatedSize - 1) / validatedSize

        val orderResponses = orders.map { mapToOrderResponse(it) }

        return PaginatedDataResponse(
            data = orderResponses,
            totalElements = totalElements,
            totalPages = totalPages,
            currentPage = validatedPage,
            pageSize = validatedSize
        )
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    fun getAllOrdersPaginated(page: Int, size: Int, status: OrderStatus?): PaginatedDataResponse<OrderResponse> {
        logger.debug("Fetching all orders - page: $page, size: $size")

        val validatedPage = maxOf(0, page)
        val validatedSize = minOf(maxOf(1, size), 100)

        val orders = orderRepository.findAllOrders(status, validatedPage, validatedSize)
        val totalElements = orderRepository.countAllOrders()
        val totalPages = if (totalElements == 0L) 0 else (totalElements + validatedSize - 1) / validatedSize

        val orderResponses = orders.map { mapToOrderResponse(it) }

        return PaginatedDataResponse(
            data = orderResponses,
            totalElements = totalElements,
            totalPages = totalPages,
            currentPage = validatedPage,
            pageSize = validatedSize
        )
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus): OrderResponse {
        logger.info("Updating order $orderId status to $newStatus")

        val order = orderRepository.findOrderById(orderId)
            ?: throw OrderNotFoundException("Order not found with ID: $orderId")

        validateStatusTransition(order.status, newStatus)

        val updatedOrder = orderRepository.updateStatus(orderId, newStatus)
        logger.info("Order status updated successfully")

        return mapToOrderResponse(updatedOrder!!)
    }

    private fun validateStatusTransition(currentStatus: OrderStatus, newStatus: OrderStatus) {
        if (currentStatus == newStatus) {
            throw InvalidOperationException("Order is already in status: $currentStatus")
        }

        val validTransitions = OrderStatus.getValidTransitions(currentStatus)

        if (newStatus !in validTransitions) {
            throw InvalidOperationException(
                "Invalid status transition from $currentStatus to $newStatus"
            )
        }
    }

    private fun mapToOrderResponse(order: Order): OrderResponse {
        val orderItems: List<Pair<OrderItem, Book>> = orderRepository.findOrderItemsAndTheirBooks(order.id!!)

        val orderItemResponses = orderItems.map { (orderItem, book) ->
            OrderItemResponse(
                id = orderItem.id!!,
                book = book,
                quantity = orderItem.quantity,
                priceAtPurchase = orderItem.priceAtPurchase,
                subtotal = orderItem.priceAtPurchase * orderItem.quantity.toBigDecimal()
            )
        }

        return OrderResponse(
            id = order.id,
            orderNumber = order.orderNumber,
            userEmail = order.userEmail,
            items = orderItemResponses,
            totalAmount = order.totalAmount,
            status = order.status,
            shippingAddress = order.shippingAddress,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt
        )
    }
}