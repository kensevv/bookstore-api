package com.lvlup.backend.integration


import com.lvlup.backend.TestFixtures
import com.lvlup.backend.TestFixtures.createTestOrder
import com.lvlup.backend.model.OrderItem
import com.lvlup.backend.model.OrderStatus
import com.lvlup.backend.model.User
import com.lvlup.backend.model.UserRole
import com.lvlup.backend.repository.BooksRepository
import com.lvlup.backend.repository.CategoriesRepository
import com.lvlup.backend.repository.OrderRepository
import com.lvlup.backend.repository.UserRepository
import org.hibernate.validator.internal.util.Contracts.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class OrderRepositoryIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var bookRepository: BooksRepository

    @Autowired
    private lateinit var categoryRepository: CategoriesRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setupAuth() {
        val auth =
            UsernamePasswordAuthenticationToken("testuser", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        SecurityContextHolder.getContext().authentication = auth
    }
    
    @Test
    fun `should create and retrieve order by id`() {
        // Given
        val user = createAndSaveUser("user@test.com")
        val order = createTestOrder(id = null, userEmail = user.email)

        // When
        val savedOrder = orderRepository.createNewOrder(order)
        val retrievedOrder = orderRepository.findOrderById(savedOrder.id!!)

        // Then
        assertNotNull(retrievedOrder)
        assertEquals(savedOrder.id, retrievedOrder!!.id)
        assertEquals(order.userEmail, retrievedOrder.userEmail)
        assertEquals(order.orderNumber, retrievedOrder.orderNumber)
        assertEquals(order.totalAmount, retrievedOrder.totalAmount)
        assertEquals(OrderStatus.PENDING, retrievedOrder.status)
    }

    @Test
    fun `should return null when order does not exist`() {
        // When
        val result = orderRepository.findOrderById(999L)

        // Then
        assertNull(result)
    }

    @Test
    fun `should find orders by user email`() {
        // Given
        val user1 = createAndSaveUser("user1@test.com")
        val user2 = createAndSaveUser("user2@test.com")

        val order1 =
            orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user1.email, orderNumber = "ORD-001"))
        val order2 =
            orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user1.email, orderNumber = "ORD-002"))
        val order3 =
            orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user2.email, orderNumber = "ORD-003"))

        // When
        val user1Orders = orderRepository.findOrdersByUserEmail(user1.email, null, 0, 10)

        // Then
        assertEquals(2, user1Orders.size)
        assertTrue(user1Orders.any { it.id == order1.id })
        assertTrue(user1Orders.any { it.id == order2.id })
        assertFalse(user1Orders.any { it.id == order3.id })
    }

    @Test
    fun `should filter orders by status`() {
        // Given
        val user = createAndSaveUser("user@test.com")

        val pendingOrder = orderRepository.createNewOrder(
            createTestOrder(id = null, userEmail = user.email, status = OrderStatus.PENDING)
        )
        val shippedOrder = orderRepository.createNewOrder(
            createTestOrder(id = null, userEmail = user.email, status = OrderStatus.SHIPPED)
        )

        orderRepository.updateStatus(shippedOrder.id!!, OrderStatus.SHIPPED)

        // When
        val pendingOrders = orderRepository.findOrdersByUserEmail(user.email, OrderStatus.PENDING, 0, 10)
        val shippedOrders = orderRepository.findOrdersByUserEmail(user.email, OrderStatus.SHIPPED, 0, 10)

        // Then
        assertEquals(1, pendingOrders.size)
        assertEquals(pendingOrder.id, pendingOrders[0].id)

        assertEquals(1, shippedOrders.size)
        assertEquals(shippedOrder.id, shippedOrders[0].id)
    }

    @Test
    fun `should paginate orders correctly`() {
        // Given
        val user = createAndSaveUser("user@test.com")

        repeat(5) { i ->
            orderRepository.createNewOrder(
                createTestOrder(
                    id = null,
                    userEmail = user.email,
                    orderNumber = "ORD-00$i"
                )
            )
        }

        // When
        val page1 = orderRepository.findOrdersByUserEmail(user.email, null, 0, 2)
        val page2 = orderRepository.findOrdersByUserEmail(user.email, null, 1, 2)
        val count = orderRepository.countOrdersByUserEmail(user.email)

        // Then
        assertEquals(2, page1.size)
        assertEquals(2, page2.size)
        assertEquals(5L, count)
    }

    @Test
    fun `should update order status successfully`() {
        // Given
        val user = createAndSaveUser("user@test.com")
        val order = orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user.email))

        // When
        val updatedOrder = orderRepository.updateStatus(order.id!!, OrderStatus.SHIPPED)

        // Then
        assertNotNull(updatedOrder)
        assertEquals(OrderStatus.SHIPPED, updatedOrder!!.status)
        assertNotEquals(order.updatedAt, updatedOrder.updatedAt) // Timestamp updated
    }

    @Test
    fun `should return null when updating non-existent order`() {
        // When
        val result = orderRepository.updateStatus(999L, OrderStatus.SHIPPED)

        // Then
        assertNull(result)
    }

    @Test
    fun `should save and retrieve order items with books`() {
        // Given
        val user = createAndSaveUser("user@test.com")
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val book = bookRepository.createBook(TestFixtures.createBookFixture(categoryId = category.id!!))
        val order = orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user.email))

        val orderItem = OrderItem(
            id = null,
            orderId = order.id!!,
            bookId = book.id!!,
            quantity = 2,
            priceAtPurchase = BigDecimal("19.99"),
            createdAt = LocalDateTime.now()
        )

        // When
        val savedOrderItem = orderRepository.saveOrderItem(orderItem)
        val itemsWithBooks = orderRepository.findOrderItemsAndTheirBooks(order.id!!)

        // Then
        assertNotNull(savedOrderItem.id)
        assertEquals(1, itemsWithBooks.size)

        val (retrievedItem, retrievedBook) = itemsWithBooks[0]
        assertEquals(savedOrderItem.id, retrievedItem.id)
        assertEquals(2, retrievedItem.quantity)
        assertEquals(BigDecimal("19.99"), retrievedItem.priceAtPurchase)
        assertEquals(book.id, retrievedBook.id)
        assertEquals(book.title, retrievedBook.title)
    }

    @Test
    fun `should return empty list when order has no items`() {
        // Given
        val user = createAndSaveUser("user@test.com")
        val order = orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user.email))

        // When
        val items = orderRepository.findOrderItemsAndTheirBooks(order.id!!)

        // Then
        assertTrue(items.isEmpty())
    }

    @Test
    fun `should handle multiple order items correctly`() {
        // Given
        val user = createAndSaveUser("user@test.com")
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val book1 =
            bookRepository.createBook(TestFixtures.createBookFixture(categoryId = category.id!!, title = "Book 1"))
        val book2 =
            bookRepository.createBook(TestFixtures.createBookFixture(categoryId = category.id!!, title = "Book 2"))
        val order = orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user.email))

        orderRepository.saveOrderItem(
            OrderItem(
                id = null, orderId = order.id!!, bookId = book1.id!!,
                quantity = 1, priceAtPurchase = BigDecimal("10.00"),
                createdAt = LocalDateTime.now()
            )
        )
        orderRepository.saveOrderItem(
            OrderItem(
                id = null, orderId = order.id!!, bookId = book2.id!!,
                quantity = 3, priceAtPurchase = BigDecimal("20.00"),
                createdAt = LocalDateTime.now()
            )
        )

        // When
        val items = orderRepository.findOrderItemsAndTheirBooks(order.id!!)

        // Then
        assertEquals(2, items.size)
        assertEquals(book1.title, items.find { it.first.bookId == book1.id }?.second?.title)
        assertEquals(book2.title, items.find { it.first.bookId == book2.id }?.second?.title)
    }

    @Test
    fun `should find all orders regardless of user`() {
        // Given
        val user1 = createAndSaveUser("user1@test.com")
        val user2 = createAndSaveUser("user2@test.com")

        orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user1.email))
        orderRepository.createNewOrder(createTestOrder(id = null, userEmail = user2.email))

        // When
        val allOrders = orderRepository.findAllOrders(null, 0, 10)
        val count = orderRepository.countAllOrders()

        // Then
        assertEquals(2, allOrders.size)
        assertEquals(2L, count)
    }


    private fun createAndSaveUser(email: String): User {
        return userRepository.createUser(
            User(
                email = email,
                passwordHash = "hashedPassword",
                firstName = "Test",
                lastName = "User",
                role = UserRole.ROLE_USER,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                deleted = false
            )
        )
    }

}