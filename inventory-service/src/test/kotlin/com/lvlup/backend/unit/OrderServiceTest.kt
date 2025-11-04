package com.lvlup.inventoryservice.unit

import com.lvlup.inventoryservice.TestFixtures
import com.lvlup.inventoryservice.TestFixtures.createTestOrder
import com.lvlup.inventoryservice.dto.CreateOrderRequest
import com.lvlup.inventoryservice.exception.*
import com.lvlup.inventoryservice.model.*
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.OrderRepository
import com.lvlup.inventoryservice.repository.ShoppingCartRepository
import com.lvlup.inventoryservice.repository.UserRepository
import com.lvlup.inventoryservice.service.OrderService
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime

class OrderServiceTest {

    private lateinit var orderService: OrderService
    private lateinit var orderRepository: OrderRepository
    private lateinit var cartRepository: ShoppingCartRepository
    private lateinit var bookRepository: BooksRepository
    private lateinit var userRepository: UserRepository

    private val testUserEmail = "test@example.com"

    @BeforeEach
    fun setup() {
        orderRepository = mockk()
        cartRepository = mockk()
        bookRepository = mockk()
        userRepository = mockk()

        orderService = OrderService(
            orderRepository,
            cartRepository,
            bookRepository,
            userRepository
        )
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ============ CREATE ORDER - SUCCESS PATH ============

    @Test
    fun `createUserOrder should create order successfully with stock deduction and cart clearing`() {
        // Given
        val request = CreateOrderRequest(shippingAddress = "123 Main Street, City, State 12345")
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book1 =
            TestFixtures.createBookFixture(
                id = 1L,
                price = BigDecimal("10.00"),
                stock = 100,
                categoryId = category.id!!
            )
        val book2 =
            TestFixtures.createBookFixture(id = 2L, price = BigDecimal("20.00"), stock = 50, categoryId = category.id!!)

        val cartItem1 =
            TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book1.id!!, quantity = 2)
        val cartItem2 = TestFixtures.createCartItemFixture(id = 2L, cartId = cart.id!!, bookId = book2.id!!, quantity = 1)

        val savedOrder = createTestOrder(
            id = 1L,
            userEmail = testUserEmail,
            orderNumber = "ORD-123456",
            totalAmount = BigDecimal("40.00"),
            status = OrderStatus.PENDING
        )

        val user = TestFixtures.createUserFixture(email = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(
            cartItem1 to book1,
            cartItem2 to book2
        )
        every { orderRepository.createNewOrder(any()) } returns savedOrder
        every { orderRepository.saveOrderItem(any()) } returns mockk()
        every { bookRepository.decrementStock(book1.id!!, 2) } returns true
        every { bookRepository.decrementStock(book2.id!!, 1) } returns true
        every { cartRepository.clearCart(cart.id!!) } just Runs
        every { orderRepository.findOrderById(savedOrder.id!!) } returns savedOrder
        every { userRepository.findUserByEmail(testUserEmail) } returns user
        every { orderRepository.findOrderItemsAndTheirBooks(savedOrder.id!!) } returns emptyList()

        // When
        val result = orderService.createUserOrder(testUserEmail, request)

        // Then
        assertNotNull(result)
        assertEquals(savedOrder.orderNumber, result.orderNumber)
        assertEquals(OrderStatus.PENDING, result.status)

        // Verify critical operations
        verify(exactly = 1) { orderRepository.createNewOrder(any()) }
        verify(exactly = 2) { orderRepository.saveOrderItem(any()) }
        verify(exactly = 1) { bookRepository.decrementStock(book1.id!!, 2) }
        verify(exactly = 1) { bookRepository.decrementStock(book2.id!!, 1) }
        verify(exactly = 1) { cartRepository.clearCart(cart.id!!) }
    }

    // ============ CREATE ORDER - ERROR CASES ============

    @Test
    fun `createUserOrder should throw EmptyCartException when user has no cart`() {
        // Given
        val request = CreateOrderRequest(shippingAddress = "123 Main Street")

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns null

        // When/Then
        assertThrows<EmptyCartException> {
            orderService.createUserOrder(testUserEmail, request)
        }

        verify(exactly = 1) { cartRepository.findCartByUserEmail(testUserEmail) }
        verify(exactly = 0) { orderRepository.createNewOrder(any()) }
    }

    @Test
    fun `createUserOrder should throw EmptyCartException when cart has no items`() {
        // Given
        val request = CreateOrderRequest(shippingAddress = "123 Main Street")
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When/Then
        val exception = assertThrows<EmptyCartException> {
            orderService.createUserOrder(testUserEmail, request)
        }

        assertTrue(exception.message!!.contains("Cannot create order from empty cart"))
        verify(exactly = 0) { orderRepository.createNewOrder(any()) }
    }

    @Test
    fun `createUserOrder should throw BookNotFoundException when cart item book is null`() {
        // Given
        val request = CreateOrderRequest(shippingAddress = "123 Main Street")
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val cartItem = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = 999L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(cartItem to null)

        // When/Then
        val exception = assertThrows<BookNotFoundException> {
            orderService.createUserOrder(testUserEmail, request)
        }

        assertTrue(exception.message!!.contains("Book with ID 999 not found"))
        verify(exactly = 0) { orderRepository.createNewOrder(any()) }
    }

    @Test
    fun `createUserOrder should throw InsufficientStockException when book stock is less than cart quantity`() {
        // Given
        val request = CreateOrderRequest(shippingAddress = "123 Main Street")
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book =
            TestFixtures.createBookFixture(id = 1L, title = "Low Stock Book", stock = 3, categoryId = category.id!!)
        val cartItem = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book.id!!, quantity = 5)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(cartItem to book)

        // When/Then
        val exception = assertThrows<InsufficientStockException> {
            orderService.createUserOrder(testUserEmail, request)
        }

        assertTrue(exception.message!!.contains("Insufficient stock for book 'Low Stock Book'"))
        assertTrue(exception.message!!.contains("Available: 3"))
        assertTrue(exception.message!!.contains("Required: 5"))
        verify(exactly = 0) { orderRepository.createNewOrder(any()) }
    }

    @Test
    fun `createUserOrder should throw InsufficientStockException when stock deduction fails due to concurrency`() {
        // Given
        val request = CreateOrderRequest(shippingAddress = "123 Main Street")
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 100, categoryId = category.id!!)
        val cartItem = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book.id!!, quantity = 2)

        val savedOrder = createTestOrder(id = 1L, userEmail = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(cartItem to book)
        every { orderRepository.createNewOrder(any()) } returns savedOrder
        every { orderRepository.saveOrderItem(any()) } returns mockk()
        every { bookRepository.decrementStock(book.id!!, 2) } returns false // Stock deduction fails

        // When/Then
        val exception = assertThrows<InsufficientStockException> {
            orderService.createUserOrder(testUserEmail, request)
        }

        assertTrue(exception.message!!.contains("Failed to deduct stock for book ID: ${book.id}"))
        assertTrue(exception.message!!.contains("Concurrent stock issue"))

        // Order was created but stock deduction failed (transaction should rollback)
        verify(exactly = 1) { bookRepository.decrementStock(book.id!!, 2) }
        verify(exactly = 0) { cartRepository.clearCart(any()) } // Cart not cleared on failure
    }

    // ============ GET ORDER BY ID ============

    @Test
    fun `getUserOrderById should return order when user owns it`() {
        // Given
        val orderId = 1L
        val order = createTestOrder(id = orderId, userEmail = testUserEmail)
        val user = TestFixtures.createUserFixture(email = testUserEmail)

        every { orderRepository.findOrderById(orderId) } returns order
        every { userRepository.findUserByEmail(testUserEmail) } returns user
        every { orderRepository.findOrderItemsAndTheirBooks(orderId) } returns emptyList()

        // When
        val result = orderService.getUserOrderById(orderId, testUserEmail)

        // Then
        assertNotNull(result)
        assertEquals(orderId, result.id)
        assertEquals(testUserEmail, result.userEmail)
    }

    @Test
    fun `getUserOrderById should throw OrderNotFoundException when order does not exist`() {
        // Given
        val orderId = 999L

        every { orderRepository.findOrderById(orderId) } returns null

        // When/Then
        val exception = assertThrows<OrderNotFoundException> {
            orderService.getUserOrderById(orderId, testUserEmail)
        }

        assertTrue(exception.message!!.contains("Order not found with ID: 999"))
    }

    @Test
    fun `getUserOrderById should throw UnauthorizedAccessException when user does not own order`() {
        // Given
        val orderId = 1L
        val order = createTestOrder(id = orderId, userEmail = "other@example.com")

        every { orderRepository.findOrderById(orderId) } returns order

        // When/Then
        val exception = assertThrows<UnauthorizedAccessException> {
            orderService.getUserOrderById(orderId, testUserEmail)
        }

        assertTrue(exception.message!!.contains("You don't have permission to view this order"))
    }

    // ============ UPDATE ORDER STATUS ============

    @Test
    fun `updateOrderStatus should update status successfully for valid transition`() {
        // Given
        val orderId = 1L
        val currentOrder = createTestOrder(id = orderId, status = OrderStatus.PENDING)
        val updatedOrder = currentOrder.copy(status = OrderStatus.SHIPPED)
        val user = TestFixtures.createUserFixture(email = testUserEmail)

        every { orderRepository.findOrderById(orderId) } returns currentOrder
        every { orderRepository.updateStatus(orderId, OrderStatus.SHIPPED) } returns updatedOrder
        every { userRepository.findUserByEmail(testUserEmail) } returns user
        every { orderRepository.findOrderItemsAndTheirBooks(orderId) } returns emptyList()

        // When
        val result = orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED)

        // Then
        assertEquals(OrderStatus.SHIPPED, result.status)
        verify(exactly = 1) { orderRepository.updateStatus(orderId, OrderStatus.SHIPPED) }
    }

    @Test
    fun `updateOrderStatus should throw OrderNotFoundException when order does not exist`() {
        // Given
        val orderId = 999L

        every { orderRepository.findOrderById(orderId) } returns null

        // When/Then
        assertThrows<OrderNotFoundException> {
            orderService.updateOrderStatus(orderId, OrderStatus.SHIPPED)
        }

        verify(exactly = 0) { orderRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `updateOrderStatus should throw InvalidOperationException when trying to set same status`() {
        // Given
        val orderId = 1L
        val order = createTestOrder(id = orderId, status = OrderStatus.PENDING)

        every { orderRepository.findOrderById(orderId) } returns order

        // When/Then
        val exception = assertThrows<InvalidOperationException> {
            orderService.updateOrderStatus(orderId, OrderStatus.PENDING)
        }

        assertTrue(exception.message!!.contains("Order is already in status: PENDING"))
        verify(exactly = 0) { orderRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `updateOrderStatus should throw InvalidOperationException for invalid status transition`() {
        // Given - Can't go from DELIVERED back to PENDING
        val orderId = 1L
        val order = createTestOrder(id = orderId, status = OrderStatus.DELIVERED)

        every { orderRepository.findOrderById(orderId) } returns order

        // When/Then
        val exception = assertThrows<InvalidOperationException> {
            orderService.updateOrderStatus(orderId, OrderStatus.PENDING)
        }

        assertTrue(exception.message!!.contains("Invalid status transition from DELIVERED to PENDING"))
        verify(exactly = 0) { orderRepository.updateStatus(any(), any()) }
    }

    @Test
    fun `updateOrderStatus should allow valid transitions based on OrderStatus rules`() {
        // Test PENDING → SHIPPED
        val order1 = createTestOrder(id = 1L, status = OrderStatus.PENDING)
        val updatedOrder1 = order1.copy(status = OrderStatus.SHIPPED)
        val user = TestFixtures.createUserFixture(email = testUserEmail)

        every { orderRepository.findOrderById(1L) } returns order1
        every { orderRepository.updateStatus(1L, OrderStatus.SHIPPED) } returns updatedOrder1
        every { userRepository.findUserByEmail(testUserEmail) } returns user
        every { orderRepository.findOrderItemsAndTheirBooks(1L) } returns emptyList()

        assertDoesNotThrow {
            orderService.updateOrderStatus(1L, OrderStatus.SHIPPED)
        }

        // Test SHIPPED → DELIVERED
        val order2 = createTestOrder(id = 2L, status = OrderStatus.SHIPPED)
        val updatedOrder2 = order2.copy(status = OrderStatus.DELIVERED)

        every { orderRepository.findOrderById(2L) } returns order2
        every { orderRepository.updateStatus(2L, OrderStatus.DELIVERED) } returns updatedOrder2
        every { userRepository.findUserByEmail(testUserEmail) } returns user
        every { orderRepository.findOrderItemsAndTheirBooks(2L) } returns emptyList()

        assertDoesNotThrow {
            orderService.updateOrderStatus(2L, OrderStatus.DELIVERED)
        }

        // Test PENDING → CANCELLED
        val order3 = createTestOrder(id = 3L, status = OrderStatus.PENDING)
        val updatedOrder3 = order3.copy(status = OrderStatus.CANCELLED)

        every { orderRepository.findOrderById(3L) } returns order3
        every { orderRepository.updateStatus(3L, OrderStatus.CANCELLED) } returns updatedOrder3
        every { userRepository.findUserByEmail(testUserEmail) } returns user
        every { orderRepository.findOrderItemsAndTheirBooks(3L) } returns emptyList()

        assertDoesNotThrow {
            orderService.updateOrderStatus(3L, OrderStatus.CANCELLED)
        }
    }
}
