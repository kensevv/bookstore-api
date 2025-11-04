package com.lvlup.inventoryservice.unit

import com.lvlup.inventoryservice.TestFixtures
import com.lvlup.inventoryservice.dto.AddToCartRequest
import com.lvlup.inventoryservice.dto.UpdateCartItemRequest
import com.lvlup.inventoryservice.exception.*
import com.lvlup.inventoryservice.model.*
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.ShoppingCartRepository
import com.lvlup.inventoryservice.service.ShoppingCartService
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ShoppingCartServiceTest {

    private lateinit var cartService: ShoppingCartService
    private lateinit var cartRepository: ShoppingCartRepository
    private lateinit var bookRepository: BooksRepository

    private val testUserEmail = "test@example.com"

    @BeforeEach
    fun setup() {
        cartRepository = mockk()
        bookRepository = mockk()
        cartService = ShoppingCartService(cartRepository, bookRepository)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // ============ GET USER CART TESTS ============

    @Test
    fun `getUserCartAndRemoveInvalidItems should return empty cart when cart exists with no items`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(cart.id!!, result.id!!)
        assertTrue(result.items.isEmpty())
        assertEquals(0, result.totalItems)
        assertEquals(BigDecimal.ZERO, result.totalAmount)

        verify(exactly = 1) { cartRepository.findCartByUserEmail(testUserEmail) }
        verify(exactly = 1) { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) }
    }

    @Test
    fun `getUserCartAndRemoveInvalidItems should create new cart when user has no cart`() {
        // Given
        val newCart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns null
        every { cartRepository.createNewShoppingCartForUser(testUserEmail) } returns newCart
        every { cartRepository.findAllCartItemsAndTheirBooks(newCart.id!!) } returns emptyList()

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(newCart.id!!, result.id!!)
        assertTrue(result.items.isEmpty())

        verify(exactly = 1) { cartRepository.findCartByUserEmail(testUserEmail) }
        verify(exactly = 1) { cartRepository.createNewShoppingCartForUser(testUserEmail) }
    }

    @Test
    fun `getUserCartAndRemoveInvalidItems should return cart with valid items`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book1 = TestFixtures.createBookFixture(
            id = 1L,
            title = "Book 1",
            price = BigDecimal("10.00"),
            categoryId = category.id!!
        )
        val book2 = TestFixtures.createBookFixture(
            id = 2L,
            title = "Book 2",
            price = BigDecimal("20.00"),
            categoryId = category.id!!
        )

        val item1 = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book1.id!!, quantity = 2)
        val item2 = TestFixtures.createCartItemFixture(id = 2L, cartId = cart.id!!, bookId = book2.id!!, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(
            item1 to book1,
            item2 to book2
        )

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(2, result.items.size)
        assertEquals(3, result.totalItems) // 2 + 1
        assertEquals(BigDecimal("40.00"), result.totalAmount) // (10*2) + (20*1)

        val firstItem = result.items[0]
        assertEquals(item1.id!!, firstItem.id!!)
        assertEquals(2, firstItem.quantity)
        assertEquals(BigDecimal("20.00"), firstItem.subtotal) // 10 * 2
    }

    @Test
    fun `getUserCartAndRemoveInvalidItems should remove items with null books and log warning`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val validBook = TestFixtures.createBookFixture(id = 1L, categoryId = category.id!!)

        val validItem =
            TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = validBook.id!!, quantity = 1)
        val invalidItem = TestFixtures.createCartItemFixture(id = 2L, cartId = cart.id!!, bookId = 999L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(
            validItem to validBook,
            invalidItem to null // Book not found
        )
        every { cartRepository.deleteCartItem(invalidItem.id!!) } just Runs

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(1, result.items.size) // Only valid item remains
        assertEquals(validItem.id!!, result.items[0].id!!)

        verify(exactly = 1) { cartRepository.deleteCartItem(invalidItem.id!!) }
    }

    @Test
    fun `getUserCartAndRemoveInvalidItems should calculate subtotal correctly`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, price = BigDecimal("15.50"), categoryId = category.id!!)
        val item = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book.id!!, quantity = 3)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(item to book)

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(BigDecimal("46.50"), result.items[0].subtotal) // 15.50 * 3
        assertEquals(BigDecimal("46.50"), result.totalAmount)
    }

    // ============ ADD ITEM TO CART TESTS ============

    @Test
    fun `addItemToUserCart should add new item to cart successfully`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 2)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 100, categoryId = category.id!!)

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns null
        every { cartRepository.addNewCartItem(any()) } returns mockk()
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        val result = cartService.addItemToUserCart(testUserEmail, request)

        // Then
        assertNotNull(result)
        verify(exactly = 1) { cartRepository.addNewCartItem(any()) }
        verify(exactly = 0) { cartRepository.updateCartItemQuantity(any(), any()) }
    }

    @Test
    fun `addItemToUserCart should create cart if user has no cart`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 2)
        val newCart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 100, categoryId = category.id!!)

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns null
        every { cartRepository.createNewShoppingCartForUser(testUserEmail) } returns newCart
        every { cartRepository.findCartItemByBookId(newCart.id!!, request.bookId) } returns null
        every { cartRepository.addNewCartItem(any()) } returns mockk()
        every { cartRepository.findAllCartItemsAndTheirBooks(newCart.id!!) } returns emptyList()

        // When
        cartService.addItemToUserCart(testUserEmail, request)

        // Then
        verify(exactly = 1) { cartRepository.addNewCartItem(any()) }
    }

    @Test
    fun `addItemToUserCart should update quantity when item already exists in cart`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 3)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 100, categoryId = category.id!!)
        val existingItem =
            TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book.id!!, quantity = 2)

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns existingItem
        every { cartRepository.updateCartItemQuantity(existingItem.id!!, 5) } just Runs // 2 + 3 = 5
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        cartService.addItemToUserCart(testUserEmail, request)

        // Then
        verify(exactly = 1) { cartRepository.updateCartItemQuantity(existingItem.id!!, 5) }
        verify(exactly = 0) { cartRepository.addNewCartItem(any()) }
    }

    @Test
    fun `addItemToUserCart should throw BookNotFoundException when book does not exist`() {
        // Given
        val request = AddToCartRequest(bookId = 999L, quantity = 1)

        every { bookRepository.findBookById(request.bookId) } returns null

        // When/Then
        val exception = assertThrows<BookNotFoundException> {
            cartService.addItemToUserCart(testUserEmail, request)
        }

        assertTrue(exception.message!!.contains("Book not found with ID: 999"))
        assertTrue(exception.message!!.contains("Can't add to cart"))

        verify(exactly = 1) { bookRepository.findBookById(request.bookId) }
        verify(exactly = 0) { cartRepository.addNewCartItem(any()) }
    }

    @Test
    fun `addItemToUserCart should throw InsufficientStockException when stock is less than requested quantity`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 10)
        val category = TestFixtures.createCategoryFixture()
        val book =
            TestFixtures.createBookFixture(id = 1L, title = "Low Stock Book", stock = 5, categoryId = category.id!!)

        every { bookRepository.findBookById(request.bookId) } returns book

        // When/Then
        val exception = assertThrows<InsufficientStockException> {
            cartService.addItemToUserCart(testUserEmail, request)
        }

        assertTrue(exception.message!!.contains("Insufficient stock for book 'Low Stock Book'"))
        assertTrue(exception.message!!.contains("Available: 5"))
        assertTrue(exception.message!!.contains("Requested: 10"))

        verify(exactly = 0) { cartRepository.addNewCartItem(any()) }
    }

    @Test
    fun `addItemToUserCart should throw InsufficientStockException when total quantity exceeds stock`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 3)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book =
            TestFixtures.createBookFixture(id = 1L, title = "Limited Book", stock = 5, categoryId = category.id!!)
        val existingItem =
            TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book.id!!, quantity = 4)

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns existingItem

        // When/Then
        val exception = assertThrows<InsufficientStockException> {
            cartService.addItemToUserCart(testUserEmail, request)
        }

        assertTrue(exception.message!!.contains("Available: 5"))
        assertTrue(exception.message!!.contains("Current in cart: 4"))
        assertTrue(exception.message!!.contains("Requested to add: 3"))

        verify(exactly = 0) { cartRepository.updateCartItemQuantity(any(), any()) }
    }

    @Test
    fun `addItemToUserCart should set timestamps when creating new cart item`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 1)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, categoryId = category.id!!)

        val itemSlot = slot<ShoppingCartItem>()
        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns null
        every { cartRepository.addNewCartItem(capture(itemSlot)) } returns mockk()
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        cartService.addItemToUserCart(testUserEmail, request)

        // Then
        val capturedItem = itemSlot.captured
        assertNotNull(capturedItem.createdAt)
        assertNotNull(capturedItem.updatedAt)
        assertEquals(capturedItem.createdAt, capturedItem.updatedAt)
        assertNull(capturedItem.id) // New item has no ID yet
    }

    @Test
    fun `addItemToUserCart should allow adding exactly the available stock quantity`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 5)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 5, categoryId = category.id!!) // Exact match

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns null
        every { cartRepository.addNewCartItem(any()) } returns mockk()
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When/Then - Should not throw exception
        assertDoesNotThrow {
            cartService.addItemToUserCart(testUserEmail, request)
        }

        verify(exactly = 1) { cartRepository.addNewCartItem(any()) }
    }

    // ============ UPDATE CART ITEM QUANTITY TESTS ============

    @Test
    fun `updateCartItemQuantity should update quantity successfully`() {
        // Given
        val cartItemId = 1L
        val request = UpdateCartItemRequest(quantity = 5)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 100, categoryId = category.id!!)
        val cartItem =
            TestFixtures.createCartItemFixture(id = cartItemId, cartId = cart.id!!, bookId = book.id!!, quantity = 3)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(cartItemId) } returns cartItem
        every { bookRepository.findBookById(cartItem.bookId) } returns book
        every { cartRepository.updateCartItemQuantity(cartItemId, request.quantity) } just Runs
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        val result = cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)

        // Then
        assertNotNull(result)
        verify(exactly = 1) { cartRepository.updateCartItemQuantity(cartItemId, 5) }
    }

    @Test
    fun `updateCartItemQuantity should throw CartNotFoundException when user has no cart`() {
        // Given
        val cartItemId = 1L
        val request = UpdateCartItemRequest(quantity = 5)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns null

        // When/Then
        assertThrows<CartNotFoundException> {
            cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)
        }

        verify(exactly = 1) { cartRepository.findCartByUserEmail(testUserEmail) }
        verify(exactly = 0) { cartRepository.updateCartItemQuantity(any(), any()) }
    }

    @Test
    fun `updateCartItemQuantity should throw ResourceNotFoundException when cart item does not exist`() {
        // Given
        val cartItemId = 999L
        val request = UpdateCartItemRequest(quantity = 5)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(cartItemId) } returns null

        // When/Then
        val exception = assertThrows<ResourceNotFoundException> {
            cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)
        }

        assertTrue(exception.message!!.contains("Cart item not found with ID: 999"))
        verify(exactly = 0) { cartRepository.updateCartItemQuantity(any(), any()) }
    }

    @Test
    fun `updateCartItemQuantity should throw UnauthorizedAccessException when item belongs to different cart`() {
        // Given
        val cartItemId = 1L
        val request = UpdateCartItemRequest(quantity = 5)
        val userCart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val otherUsersCartItem =
            TestFixtures.createCartItemFixture(id = cartItemId, cartId = 999L, bookId = 1L, quantity = 3)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns userCart
        every { cartRepository.findCartItemById(cartItemId) } returns otherUsersCartItem

        // When/Then
        val exception = assertThrows<UnauthorizedAccessException> {
            cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)
        }

        assertTrue(exception.message!!.contains("don't have permission to modify this cart item"))
        verify(exactly = 0) { cartRepository.updateCartItemQuantity(any(), any()) }
    }

    @Test
    fun `updateCartItemQuantity should throw BookNotFoundException when book does not exist`() {
        // Given
        val cartItemId = 1L
        val request = UpdateCartItemRequest(quantity = 5)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val cartItem =
            TestFixtures.createCartItemFixture(id = cartItemId, cartId = cart.id!!, bookId = 999L, quantity = 3)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(cartItemId) } returns cartItem
        every { bookRepository.findBookById(cartItem.bookId) } returns null

        // When/Then
        val exception = assertThrows<BookNotFoundException> {
            cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)
        }

        assertEquals("Book not found", exception.message)
        verify(exactly = 0) { cartRepository.updateCartItemQuantity(any(), any()) }
    }

    @Test
    fun `updateCartItemQuantity should throw InsufficientStockException when stock is less than requested`() {
        // Given
        val cartItemId = 1L
        val request = UpdateCartItemRequest(quantity = 10)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book =
            TestFixtures.createBookFixture(id = 1L, title = "Limited Stock", stock = 5, categoryId = category.id!!)
        val cartItem =
            TestFixtures.createCartItemFixture(id = cartItemId, cartId = cart.id!!, bookId = book.id!!, quantity = 3)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(cartItemId) } returns cartItem
        every { bookRepository.findBookById(cartItem.bookId) } returns book

        // When/Then
        val exception = assertThrows<InsufficientStockException> {
            cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)
        }

        assertTrue(exception.message!!.contains("Insufficient stock for book 'Limited Stock'"))
        assertTrue(exception.message!!.contains("Available: 5"))
        assertTrue(exception.message!!.contains("Requested: 10"))

        verify(exactly = 0) { cartRepository.updateCartItemQuantity(any(), any()) }
    }

    @Test
    fun `updateCartItemQuantity should allow updating to exact available stock`() {
        // Given
        val cartItemId = 1L
        val request = UpdateCartItemRequest(quantity = 5)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 5, categoryId = category.id!!)
        val cartItem =
            TestFixtures.createCartItemFixture(id = cartItemId, cartId = cart.id!!, bookId = book.id!!, quantity = 2)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(cartItemId) } returns cartItem
        every { bookRepository.findBookById(cartItem.bookId) } returns book
        every { cartRepository.updateCartItemQuantity(cartItemId, 5) } just Runs
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When/Then - Should not throw
        assertDoesNotThrow {
            cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)
        }

        verify(exactly = 1) { cartRepository.updateCartItemQuantity(cartItemId, 5) }
    }

    // ============ REMOVE ITEM FROM CART TESTS ============

    @Test
    fun `removeItemFromUserCart should remove item successfully`() {
        // Given
        val itemId = 1L
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val cartItem = TestFixtures.createCartItemFixture(id = itemId, cartId = cart.id!!, bookId = 1L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(itemId) } returns cartItem
        every { cartRepository.deleteCartItem(itemId) } just Runs
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        val result = cartService.removeItemFromUserCart(testUserEmail, itemId)

        // Then
        assertNotNull(result)
        verify(exactly = 1) { cartRepository.deleteCartItem(itemId) }
    }

    @Test
    fun `removeItemFromUserCart should throw CartNotFoundException when user has no cart`() {
        // Given
        val itemId = 1L

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns null

        // When/Then
        assertThrows<CartNotFoundException> {
            cartService.removeItemFromUserCart(testUserEmail, itemId)
        }

        verify(exactly = 1) { cartRepository.findCartByUserEmail(testUserEmail) }
        verify(exactly = 0) { cartRepository.deleteCartItem(any()) }
    }

    @Test
    fun `removeItemFromUserCart should throw ResourceNotFoundException when item does not exist`() {
        // Given
        val itemId = 999L
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(itemId) } returns null

        // When/Then
        val exception = assertThrows<ResourceNotFoundException> {
            cartService.removeItemFromUserCart(testUserEmail, itemId)
        }

        assertTrue(exception.message!!.contains("Cart item not found with ID: 999"))
        verify(exactly = 0) { cartRepository.deleteCartItem(any()) }
    }

    @Test
    fun `removeItemFromUserCart should throw UnauthorizedAccessException when item belongs to different cart`() {
        // Given
        val itemId = 1L
        val userCart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val otherUsersCartItem =
            TestFixtures.createCartItemFixture(id = itemId, cartId = 999L, bookId = 1L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns userCart
        every { cartRepository.findCartItemById(itemId) } returns otherUsersCartItem

        // When/Then
        val exception = assertThrows<UnauthorizedAccessException> {
            cartService.removeItemFromUserCart(testUserEmail, itemId)
        }

        assertTrue(exception.message!!.contains("don't have permission to modify this cart item"))
        verify(exactly = 0) { cartRepository.deleteCartItem(any()) }
    }

    @Test
    fun `removeItemFromUserCart should verify ownership before deletion`() {
        // Given
        val itemId = 1L
        val userCart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val otherUsersCartItem = TestFixtures.createCartItemFixture(id = itemId, cartId = 2L, bookId = 1L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns userCart
        every { cartRepository.findCartItemById(itemId) } returns otherUsersCartItem

        // When/Then
        assertThrows<UnauthorizedAccessException> {
            cartService.removeItemFromUserCart(testUserEmail, itemId)
        }

        // Verify ownership was checked before attempting deletion
        verifyOrder {
            cartRepository.findCartByUserEmail(testUserEmail)
            cartRepository.findCartItemById(itemId)
            // deleteCartItem should NOT be called
        }
        verify(exactly = 0) { cartRepository.deleteCartItem(any()) }
    }

    // ============ EDGE CASES AND INTEGRATION SCENARIOS ============

    @Test
    fun `cart operations should handle multiple items correctly`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book1 = TestFixtures.createBookFixture(id = 1L, price = BigDecimal("10.00"), categoryId = category.id!!)
        val book2 = TestFixtures.createBookFixture(id = 2L, price = BigDecimal("15.00"), categoryId = category.id!!)
        val book3 = TestFixtures.createBookFixture(id = 3L, price = BigDecimal("20.00"), categoryId = category.id!!)

        val item1 = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = 1L, quantity = 2)
        val item2 = TestFixtures.createCartItemFixture(id = 2L, cartId = cart.id!!, bookId = 2L, quantity = 3)
        val item3 = TestFixtures.createCartItemFixture(id = 3L, cartId = cart.id!!, bookId = 3L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(
            item1 to book1,
            item2 to book2,
            item3 to book3
        )

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(3, result.items.size)
        assertEquals(6, result.totalItems) // 2 + 3 + 1
        // (10*2) + (15*3) + (20*1) = 20 + 45 + 20 = 85
        assertEquals(BigDecimal("85.00"), result.totalAmount)
    }

    @Test
    fun `cart should handle mix of valid and invalid items`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val validBook = TestFixtures.createBookFixture(id = 1L, price = BigDecimal("10.00"), categoryId = category.id!!)

        val validItem = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = 1L, quantity = 2)
        val invalidItem1 = TestFixtures.createCartItemFixture(id = 2L, cartId = cart.id!!, bookId = 999L, quantity = 1)
        val invalidItem2 = TestFixtures.createCartItemFixture(id = 3L, cartId = cart.id!!, bookId = 888L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(
            validItem to validBook,
            invalidItem1 to null,
            invalidItem2 to null
        )
        every { cartRepository.deleteCartItem(invalidItem1.id!!) } just Runs
        every { cartRepository.deleteCartItem(invalidItem2.id!!) } just Runs

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(1, result.items.size)
        assertEquals(2, result.totalItems)
        assertEquals(BigDecimal("20.00"), result.totalAmount)

        verify(exactly = 1) { cartRepository.deleteCartItem(invalidItem1.id!!) }
        verify(exactly = 1) { cartRepository.deleteCartItem(invalidItem2.id!!) }
    }

    @Test
    fun `addItemToUserCart should handle zero stock correctly`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 1)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 0, categoryId = category.id!!)

        every { bookRepository.findBookById(request.bookId) } returns book

        // When/Then
        assertThrows<InsufficientStockException> {
            cartService.addItemToUserCart(testUserEmail, request)
        }
    }

    @Test
    fun `updateCartItemQuantity should handle reducing quantity`() {
        // Given
        val cartItemId = 1L
        val request = UpdateCartItemRequest(quantity = 2) // Reducing from 5 to 2
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 10, categoryId = category.id!!)
        val cartItem =
            TestFixtures.createCartItemFixture(id = cartItemId, cartId = cart.id!!, bookId = book.id!!, quantity = 5)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemById(cartItemId) } returns cartItem
        every { bookRepository.findBookById(cartItem.bookId) } returns book
        every { cartRepository.updateCartItemQuantity(cartItemId, 2) } just Runs
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        cartService.updateCartItemQuantity(testUserEmail, cartItemId, request)

        // Then
        verify(exactly = 1) { cartRepository.updateCartItemQuantity(cartItemId, 2) }
    }

    @Test
    fun `cart total calculations should handle decimal prices correctly`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book1 = TestFixtures.createBookFixture(id = 1L, price = BigDecimal("12.99"), categoryId = category.id!!)
        val book2 = TestFixtures.createBookFixture(id = 2L, price = BigDecimal("7.50"), categoryId = category.id!!)

        val item1 = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = 1L, quantity = 3)
        val item2 = TestFixtures.createCartItemFixture(id = 2L, cartId = cart.id!!, bookId = 2L, quantity = 2)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(
            item1 to book1,
            item2 to book2
        )

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        // (12.99 * 3) + (7.50 * 2) = 38.97 + 15.00 = 53.97
        assertEquals(BigDecimal("53.97"), result.totalAmount)
        assertEquals(BigDecimal("38.97"), result.items[0].subtotal)
        assertEquals(BigDecimal("15.00"), result.items[1].subtotal)
    }

    @Test
    fun `getUserCartAndRemoveInvalidItems should return correct availableStock for each item`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book1 = TestFixtures.createBookFixture(id = 1L, stock = 100, categoryId = category.id!!)
        val book2 = TestFixtures.createBookFixture(id = 2L, stock = 5, categoryId = category.id!!)

        val item1 = TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = 1L, quantity = 2)
        val item2 = TestFixtures.createCartItemFixture(id = 2L, cartId = cart.id!!, bookId = 2L, quantity = 1)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns listOf(
            item1 to book1,
            item2 to book2
        )

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(100, result.items[0].availableStock)
        assertEquals(5, result.items[1].availableStock)
    }

    @Test
    fun `addItemToUserCart should handle adding item when cart is empty`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 1)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, categoryId = category.id!!)

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns null
        every { cartRepository.addNewCartItem(any()) } returns mockk()
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        val result = cartService.addItemToUserCart(testUserEmail, request)

        // Then
        assertNotNull(result)
        assertEquals(0, result.items.size) // Empty because we return fresh cart
        verify(exactly = 1) { cartRepository.addNewCartItem(any()) }
    }

    @Test
    fun `cart operations should work with different user emails`() {
        // Given
        val user1Email = "user1@example.com"
        val user2Email = "user2@example.com"
        val cart1 = TestFixtures.createCartFixture(id = 1L, userEmail = user1Email)
        val cart2 = TestFixtures.createCartFixture(id = 2L, userEmail = user2Email)

        every { cartRepository.findCartByUserEmail(user1Email) } returns cart1
        every { cartRepository.findCartByUserEmail(user2Email) } returns cart2
        every { cartRepository.findAllCartItemsAndTheirBooks(cart1.id!!) } returns emptyList()
        every { cartRepository.findAllCartItemsAndTheirBooks(cart2.id!!) } returns emptyList()

        // When
        val result1 = cartService.getUserCartAndRemoveInvalidItems(user1Email)
        val result2 = cartService.getUserCartAndRemoveInvalidItems(user2Email)

        // Then
        assertEquals(cart1.id!!, result1.id!!)
        assertEquals(cart2.id!!, result2.id!!)
        assertNotEquals(result1.id!!, result2.id!!)
    }

    @Test
    fun `addItemToUserCart should respect exact stock boundary`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 1)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 1, categoryId = category.id!!) // Only 1 in stock

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns null
        every { cartRepository.addNewCartItem(any()) } returns mockk()
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When/Then - Should succeed
        assertDoesNotThrow {
            cartService.addItemToUserCart(testUserEmail, request)
        }
    }

    @Test
    fun `addItemToUserCart with existing item should respect total stock boundary`() {
        // Given
        val request = AddToCartRequest(bookId = 1L, quantity = 5)
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)
        val category = TestFixtures.createCategoryFixture()
        val book = TestFixtures.createBookFixture(id = 1L, stock = 10, categoryId = category.id!!)
        val existingItem =
            TestFixtures.createCartItemFixture(id = 1L, cartId = cart.id!!, bookId = book.id!!, quantity = 5)

        every { bookRepository.findBookById(request.bookId) } returns book
        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findCartItemByBookId(cart.id!!, request.bookId) } returns existingItem
        every { cartRepository.updateCartItemQuantity(existingItem.id!!, 10) } just Runs
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When/Then - Should succeed (5 + 5 = 10 == stock)
        assertDoesNotThrow {
            cartService.addItemToUserCart(testUserEmail, request)
        }

        verify(exactly = 1) { cartRepository.updateCartItemQuantity(existingItem.id!!, 10) }
    }

    @Test
    fun `empty cart should have zero total amount and items`() {
        // Given
        val cart = TestFixtures.createCartFixture(id = 1L, userEmail = testUserEmail)

        every { cartRepository.findCartByUserEmail(testUserEmail) } returns cart
        every { cartRepository.findAllCartItemsAndTheirBooks(cart.id!!) } returns emptyList()

        // When
        val result = cartService.getUserCartAndRemoveInvalidItems(testUserEmail)

        // Then
        assertEquals(BigDecimal.ZERO, result.totalAmount)
        assertEquals(0, result.totalItems)
        assertTrue(result.items.isEmpty())
    }
}