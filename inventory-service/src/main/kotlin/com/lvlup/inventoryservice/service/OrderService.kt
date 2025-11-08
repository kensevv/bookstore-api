package com.lvlup.inventoryservice.service

import com.lvlup.inventoryservice.dto.CreateOrderRequest
import com.lvlup.inventoryservice.dto.OrderItemResponse
import com.lvlup.inventoryservice.dto.OrderResponse
import com.lvlup.inventoryservice.exception.BookNotFoundException
import com.lvlup.inventoryservice.exception.EmptyCartException
import com.lvlup.inventoryservice.exception.InsufficientStockException
import com.lvlup.inventoryservice.exception.InvalidOperationException
import com.lvlup.inventoryservice.exception.OrderNotFoundException
import com.lvlup.inventoryservice.model.Book
import com.lvlup.inventoryservice.model.Order
import com.lvlup.inventoryservice.model.OrderStatus
import com.lvlup.inventoryservice.model.ShoppingCart
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.OrderRepository
import com.lvlup.inventoryservice.repository.ShoppingCartItemRepository
import com.lvlup.inventoryservice.repository.ShoppingCartRepository
import dto.PaginatedDataResponse
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
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
    private val cartItemRepository: ShoppingCartItemRepository,
    private val bookRepository: BooksRepository,
) {

    private val logger = KotlinLogging.logger {}

    @Transactional
    fun createUserOrder(userEmail: String, request: CreateOrderRequest): OrderResponse {
        logger.info("Creating shopping cart order for user $userEmail")

        val userShoppingCart = cartRepository.findByUserEmail(userEmail)

        if (userShoppingCart == null || userShoppingCart.items.isEmpty()) {
            throw EmptyCartException("Cannot create order from empty cart")
        }

        // Validate cart items and prepare order data
        val orderItemsData = validateCartAndGetItems(userShoppingCart)

        val createdAt = LocalDateTime.now()
        val savedOrder = createAndSaveOrder(userEmail, orderItemsData, request.shippingAddress, createdAt)

        processOrderItemsAndHandleStock(orderItemsData, savedOrder, createdAt)

        userShoppingCart.clearItems()
        cartRepository.save(userShoppingCart)

        logger.info("Order placement completed successfully: ${savedOrder.orderNumber}")

        return mapToOrderResponse(savedOrder)
    }

    private fun processOrderItemsAndHandleStock(
        shoppingCartOrderItemsData: List<Triple<Book, Int, BigDecimal>>,
        savedOrder: Order,
        createdAt: LocalDateTime
    ) {
        shoppingCartOrderItemsData.forEach { (book, quantity, price) ->
            savedOrder.addOrderItem(book, quantity, price, createdAt)

            // Deduct stock
            val rowsAffected = bookRepository.decrementStock(book.id!!, quantity, createdAt) // conditional update
            if (rowsAffected == 0) {
                throw InsufficientStockException(
                    "Failed to deduct stock for book ID: $book. Concurrent stock issue."
                )
            }
        }
    }


    private fun validateCartAndGetItems(cart: ShoppingCart): List<Triple<Book, Int, BigDecimal>> {
        val orderItemsData = cart.items.map { item ->
            if (item.book == null) {
                throw BookNotFoundException("Book with ID ${item.bookId} not found")
            }

            if (item.book.stock < item.quantity) {
                throw InsufficientStockException(
                    "Insufficient stock for book '${item.book.title}'. " +
                            "Available: ${item.book.stock}, Required: ${item.quantity}"
                )
            }

            Triple(item.book, item.quantity, item.book.price)
        }

        return orderItemsData
    }

    private fun createAndSaveOrder(
        userEmail: String,
        orderItemsData: List<Triple<Book, Int, BigDecimal>>,
        shippingAddress: String,
        createdAt: LocalDateTime
    ): Order {
        val newOrderNumber = generateNewOrderNumber()
        val order = Order(
            userEmail = userEmail,
            orderNumber = newOrderNumber,
            totalAmount = orderItemsData.sumOf { it.second.toBigDecimal() * it.third },
            status = OrderStatus.PENDING,
            shippingAddress = shippingAddress.trim(),
            createdAt = createdAt,
            updatedAt = createdAt
        )

        val savedOrder = orderRepository.save(order)
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

        val order = orderRepository.findByIdAndUserEmail(orderId, userEmail).orElseThrow {
            OrderNotFoundException("Order not found with ID: $orderId for user $userEmail")
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

        val pageResult = orderRepository.findAllByUserEmailAndStatus(
            userEmail,
            status,
            PageRequest.of(
                maxOf(0, page),
                minOf(maxOf(1, size), 100),
                sortByKProperty(Order::createdAt)
            )

        )

        return pageResult.toPaginatedDataResponse(
            pageResult.content.map { mapToOrderResponse(it) },
        )

    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    fun getAllOrdersPaginated(page: Int, size: Int, status: OrderStatus?): PaginatedDataResponse<OrderResponse> {
        logger.debug("Fetching all orders - page: $page, size: $size")

        val pageResult = orderRepository.findAllByStatus(
            status,
            PageRequest.of(
                maxOf(0, page),
                minOf(maxOf(1, size), 100),
                sortByKProperty(Order::createdAt)
            )

        )

        return pageResult.toPaginatedDataResponse(
            pageResult.content.map { mapToOrderResponse(it) },
        )
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun updateOrderStatus(orderId: Long, newStatus: OrderStatus): OrderResponse {
        logger.info("Updating order $orderId status to $newStatus")

        val order = orderRepository.findById(orderId).orElseThrow {
            OrderNotFoundException("Order not found with ID: $orderId")
        }

        validateStatusTransition(order.status, newStatus)

        order.status = newStatus
        orderRepository.save(order)
        logger.info("Order status updated successfully")

        return mapToOrderResponse(order!!)
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
        val orderItems = order.items

        return OrderResponse(
            id = order.id!!,
            orderNumber = order.orderNumber,
            userEmail = order.userEmail,
            items = orderItems.map { orderItem ->
                OrderItemResponse(
                    id = orderItem.id!!,
                    book = orderItem.book,
                    quantity = orderItem.quantity,
                    priceAtPurchase = orderItem.priceAtPurchase,
                    subtotal = orderItem.priceAtPurchase * orderItem.quantity.toBigDecimal()
                )
            },
            totalAmount = order.totalAmount,
            status = order.status,
            shippingAddress = order.shippingAddress,
            createdAt = order.createdAt!!,
            updatedAt = order.updatedAt!!
        )
    }
}

