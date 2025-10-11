package com.lvlup.backend.unit

import com.lvlup.backend.TestFixtures
import com.lvlup.backend.service.BookService
import com.lvlup.backend.dto.BookRequest
import com.lvlup.backend.exception.BookNotFoundException
import com.lvlup.backend.exception.CategoryNotFoundException
import com.lvlup.backend.model.Book
import com.lvlup.backend.repository.BooksRepository
import com.lvlup.backend.repository.CategoriesRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime

class BookServiceTest {

    private lateinit var bookService: BookService
    private lateinit var booksRepository: BooksRepository
    private lateinit var categoriesRepository: CategoriesRepository

    @BeforeEach
    fun setup() {
        booksRepository = mockk()
        categoriesRepository = mockk()
        bookService = BookService(booksRepository, categoriesRepository)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createBook should create book successfully with valid data`() {
        // Given
        val category = TestFixtures.createCategoryFixture(id = 1L, name = "Fiction")
        val request = BookRequest(
            title = "Clean Code",
            author = "Robert Martin",
            description = "A handbook of agile software craftsmanship",
            price = BigDecimal("45.99"),
            stock = 100,
            categoryId = category.id!!,
            coverImageUrl = "https://example.com/cover.jpg"
        )

        val savedBook = TestFixtures.createBookFixture(
            id = 1L,
            title = request.title,
            author = request.author,
            categoryId = category.id!!,
            price = request.price,
            stock = request.stock
        )

        every { categoriesRepository.findCategoryById(category.id!!) } returns category
        every { booksRepository.createBook(any()) } returns savedBook

        // When
        val result = bookService.createBook(request)

        // Then
        assertNotNull(result)
        assertEquals(1L, result.id)
        assertEquals("Clean Code", result.title)
        assertEquals("Robert Martin", result.author)
        assertEquals(BigDecimal("45.99"), result.price)
        assertEquals(100, result.stock)
        assertEquals(category.id, result.category.id)
        assertFalse(result.deleted)

        verify(exactly = 1) { categoriesRepository.findCategoryById(category.id!!) }
        verify(exactly = 1) { booksRepository.createBook(any()) }
    }

    @Test
    fun `createBook should set deleted flag to false`() {
        // Given
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val request = TestFixtures.createBookRequest(categoryId = category.id!!)

        val bookSlot = slot<Book>()
        every { categoriesRepository.findCategoryById(category.id!!) } returns category
        every { booksRepository.createBook(capture(bookSlot)) } answers { bookSlot.captured.copy(id = 1L) }

        // When
        bookService.createBook(request)

        // Then
        assertFalse(bookSlot.captured.deleted)
    }

    @Test
    fun `createBook should set createdAt and updatedAt to same timestamp`() {
        // Given
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val request = TestFixtures.createBookRequest(categoryId = category.id!!)

        val bookSlot = slot<Book>()
        every { categoriesRepository.findCategoryById(category.id!!) } returns category
        every { booksRepository.createBook(capture(bookSlot)) } answers { bookSlot.captured.copy(id = 1L) }

        // When
        bookService.createBook(request)

        // Then
        val capturedBook = bookSlot.captured
        assertNotNull(capturedBook.createdAt)
        assertNotNull(capturedBook.updatedAt)
        assertEquals(capturedBook.createdAt, capturedBook.updatedAt)
    }

    @Test
    fun `createBook should throw CategoryNotFoundException when category does not exist`() {
        // Given
        val request = TestFixtures.createBookRequest(categoryId = 999L)

        every { categoriesRepository.findCategoryById(999L) } returns null

        // When/Then
        val exception = assertThrows<CategoryNotFoundException> {
            bookService.createBook(request)
        }

        assertTrue(exception.message!!.contains("Category not found with ID: 999"))

        verify(exactly = 1) { categoriesRepository.findCategoryById(999L) }
        verify(exactly = 0) { booksRepository.createBook(any()) }
    }

    // ============ UPDATE BOOK TESTS ============

    @Test
    fun `updateBook should update book successfully with valid data`() {
        // Given
        val bookId = 1L
        val oldCategory = TestFixtures.createCategoryFixture(id = 1L, name = "Old Category")
        val newCategory = TestFixtures.createCategoryFixture(id = 2L, name = "New Category")

        val existingBook = TestFixtures.createBookFixture(
            id = bookId,
            title = "Old Title",
            author = "Old Author",
            categoryId = oldCategory.id!!,
            price = BigDecimal("20.00"),
            stock = 50
        )

        val request = BookRequest(
            title = "Updated Title",
            author = "Updated Author",
            description = "Updated description",
            price = BigDecimal("30.00"),
            stock = 75,
            categoryId = newCategory.id!!,
            coverImageUrl = "https://example.com/updated.jpg"
        )

        val updatedBook = existingBook.copy(
            title = request.title,
            author = request.author,
            description = request.description,
            price = request.price,
            stock = request.stock,
            category = newCategory,
            coverImageUrl = request.coverImageUrl
        )

        every { booksRepository.findBookById(bookId) } returns existingBook
        every { categoriesRepository.findCategoryById(newCategory.id!!) } returns newCategory
        every { booksRepository.updateBook(any()) } returns updatedBook

        // When
        val result = bookService.updateBook(bookId, request)

        // Then
        assertEquals("Updated Title", result.title)
        assertEquals("Updated Author", result.author)
        assertEquals("Updated description", result.description)
        assertEquals(BigDecimal("30.00"), result.price)
        assertEquals(75, result.stock)
        assertEquals(newCategory, result.category)

        verify(exactly = 1) { booksRepository.findBookById(bookId) }
        verify(exactly = 1) { categoriesRepository.findCategoryById(newCategory.id!!) }
        verify(exactly = 1) { booksRepository.updateBook(any()) }
    }

    @Test
    fun `updateBook should preserve createdAt and deleted flag`() {
        // Given
        val bookId = 1L
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val originalCreatedAt = LocalDateTime.now().minusDays(10)

        val existingBook = TestFixtures.createBookFixture(
            id = bookId,
            categoryId = category.id!!,
            deleted = false,
            createdAt = originalCreatedAt
        )

        val request = TestFixtures.createBookRequest(categoryId = category.id!!)

        val bookSlot = slot<Book>()
        every { booksRepository.findBookById(bookId) } returns existingBook
        every { categoriesRepository.findCategoryById(category.id!!) } returns category
        every { booksRepository.updateBook(capture(bookSlot)) } answers { bookSlot.captured }

        // When
        bookService.updateBook(bookId, request)

        // Then
        val capturedBook = bookSlot.captured
        assertEquals(originalCreatedAt, capturedBook.createdAt)
        assertFalse(capturedBook.deleted)
    }

    @Test
    fun `updateBook should throw BookNotFoundException when book does not exist`() {
        // Given
        val bookId = 999L
        val request = TestFixtures.createBookRequest(categoryId = 1L)

        every { booksRepository.findBookById(bookId) } returns null

        // When/Then
        val exception = assertThrows<BookNotFoundException> {
            bookService.updateBook(bookId, request)
        }

        assertTrue(exception.message!!.contains("Book not found with ID: 999"))

        verify(exactly = 1) { booksRepository.findBookById(bookId) }
        verify(exactly = 0) { categoriesRepository.findCategoryById(any()) }
        verify(exactly = 0) { booksRepository.updateBook(any()) }
    }

    @Test
    fun `updateBook should throw CategoryNotFoundException when category does not exist`() {
        // Given
        val bookId = 1L
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val existingBook = TestFixtures.createBookFixture(id = bookId, categoryId = category.id!!)
        val request = TestFixtures.createBookRequest(categoryId = 999L)

        every { booksRepository.findBookById(bookId) } returns existingBook
        every { categoriesRepository.findCategoryById(999L) } returns null

        // When/Then
        val exception = assertThrows<CategoryNotFoundException> {
            bookService.updateBook(bookId, request)
        }

        assertTrue(exception.message!!.contains("Category not found with ID: 999"))

        verify(exactly = 1) { booksRepository.findBookById(bookId) }
        verify(exactly = 1) { categoriesRepository.findCategoryById(999L) }
        verify(exactly = 0) { booksRepository.updateBook(any()) }
    }

    @Test
    fun `updateBook should throw BookNotFoundException when repository update returns null`() {
        // Given
        val bookId = 1L
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val existingBook = TestFixtures.createBookFixture(id = bookId, categoryId = category.id!!)
        val request = TestFixtures.createBookRequest(categoryId = category.id!!)

        every { booksRepository.findBookById(bookId) } returns existingBook
        every { categoriesRepository.findCategoryById(category.id!!) } returns category
        every { booksRepository.updateBook(any()) } returns null

        // When/Then
        val exception = assertThrows<BookNotFoundException> {
            bookService.updateBook(bookId, request)
        }

        assertTrue(exception.message!!.contains("Book not found with ID: 1"))

        verify(exactly = 1) { booksRepository.updateBook(any()) }
    }

    // ============ DELETE BOOK TESTS ============

    @Test
    fun `deleteBook should delete book successfully`() {
        // Given
        val bookId = 1L
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val existingBook = TestFixtures.createBookFixture(id = bookId, categoryId = category.id!!)

        every { booksRepository.findBookById(bookId) } returns existingBook
        every { booksRepository.deleteBookById(bookId) } just Runs

        // When
        bookService.deleteBook(bookId)

        // Then
        verify(exactly = 1) { booksRepository.findBookById(bookId) }
        verify(exactly = 1) { booksRepository.deleteBookById(bookId) }
    }

    @Test
    fun `deleteBook should throw BookNotFoundException when book does not exist`() {
        // Given
        val bookId = 999L

        every { booksRepository.findBookById(bookId) } returns null

        // When/Then
        val exception = assertThrows<BookNotFoundException> {
            bookService.deleteBook(bookId)
        }

        assertTrue(exception.message!!.contains("Book not found with ID: 999"))

        verify(exactly = 1) { booksRepository.findBookById(bookId) }
        verify(exactly = 0) { booksRepository.deleteBookById(any()) }
    }

    @Test
    fun `deleteBook should verify book exists before attempting deletion`() {
        // Given
        val bookId = 1L

        every { booksRepository.findBookById(bookId) } returns null

        // When/Then
        assertThrows<BookNotFoundException> {
            bookService.deleteBook(bookId)
        }

        // Verify we checked existence first
        verifyOrder {
            booksRepository.findBookById(bookId)
            // deleteBookById should NOT be called
        }
        verify(exactly = 0) { booksRepository.deleteBookById(any()) }
    }

    // ============ GET BOOK BY ID TESTS ============

    @Test
    fun `getBookById should return book when it exists`() {
        // Given
        val bookId = 1L
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val book = TestFixtures.createBookFixture(id = bookId, categoryId = category.id!!, title = "Test Book")

        every { booksRepository.findBookById(bookId) } returns book

        // When
        val result = bookService.getBookById(bookId)

        // Then
        assertNotNull(result)
        assertEquals(bookId, result.id)
        assertEquals("Test Book", result.title)

        verify(exactly = 1) { booksRepository.findBookById(bookId) }
    }

    @Test
    fun `getBookById should throw BookNotFoundException when book does not exist`() {
        // Given
        val bookId = 999L

        every { booksRepository.findBookById(bookId) } returns null

        // When/Then
        val exception = assertThrows<BookNotFoundException> {
            bookService.getBookById(bookId)
        }

        assertTrue(exception.message!!.contains("Book not found with ID: 999"))

        verify(exactly = 1) { booksRepository.findBookById(bookId) }
    }

    // ============ GET ALL BOOKS PAGINATED TESTS ============

    @Test
    fun `getAllBooksPaginated should return paginated books with valid parameters`() {
        // Given
        val category = TestFixtures.createCategoryFixture(id = 1L)
        val books = listOf(
            TestFixtures.createBookFixture(id = 1L, categoryId = category.id!!, title = "Book 1"),
            TestFixtures.createBookFixture(id = 2L, categoryId = category.id!!, title = "Book 2"),
            TestFixtures.createBookFixture(id = 3L, categoryId = category.id!!, title = "Book 3")
        )

        every { booksRepository.findBooksPaginated(0, 10, null, null, null, null, null) } returns books
        every { booksRepository.getBooksCount(null, null, null, null, null) } returns 3

        // When
        val result = bookService.getAllBooksPaginated(
            page = 0,
            size = 10,
            title = null,
            author = null,
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(3, result.data.size)
        assertEquals(3, result.totalElements)
        assertEquals(1, result.totalPages)
        assertEquals(0, result.currentPage)
        assertEquals(10, result.pageSize)

        verify(exactly = 1) { booksRepository.findBooksPaginated(0, 10, null, null, null, null, null) }
        verify(exactly = 1) { booksRepository.getBooksCount(null, null, null, null, null) }
    }

    @Test
    fun `getAllBooksPaginated should validate and correct negative page number`() {
        // Given
        every { booksRepository.findBooksPaginated(0, 10, null, null, null, null, null) } returns emptyList()
        every { booksRepository.getBooksCount(null, null, null, null, null) } returns 0

        // When
        val result = bookService.getAllBooksPaginated(
            page = -5,
            size = 10,
            title = null,
            author = null,
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(0, result.currentPage)
        verify { booksRepository.findBooksPaginated(0, 10, null, null, null, null, null) }
    }

    @Test
    fun `getAllBooksPaginated should validate and correct page size below minimum`() {
        // Given
        every { booksRepository.findBooksPaginated(0, 1, null, null, null, null, null) } returns emptyList()
        every { booksRepository.getBooksCount(null, null, null, null, null) } returns 0

        // When
        val result = bookService.getAllBooksPaginated(
            page = 0,
            size = 0,
            title = null,
            author = null,
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(1, result.pageSize)
        verify { booksRepository.findBooksPaginated(0, 1, null, null, null, null, null) }
    }

    @Test
    fun `getAllBooksPaginated should limit page size to maximum of 100`() {
        // Given
        every { booksRepository.findBooksPaginated(0, 100, null, null, null, null, null) } returns emptyList()
        every { booksRepository.getBooksCount(null, null, null, null, null) } returns 0

        // When
        val result = bookService.getAllBooksPaginated(
            page = 0,
            size = 500,
            title = null,
            author = null,
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(100, result.pageSize)
        verify { booksRepository.findBooksPaginated(0, 100, null, null, null, null, null) }
    }

    @Test
    fun `getAllBooksPaginated should calculate total pages correctly`() {
        // Given
        every {
            booksRepository.findBooksPaginated(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns emptyList()
        every { booksRepository.getBooksCount(any(), any(), any(), any(), any()) } returns 25

        // When
        val result = bookService.getAllBooksPaginated(
            page = 0,
            size = 10,
            title = null,
            author = null,
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(3, result.totalPages) // 25 items / 10 per page = 3 pages
    }

    @Test
    fun `getAllBooksPaginated should return 0 total pages when no books exist`() {
        // Given
        every {
            booksRepository.findBooksPaginated(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns emptyList()
        every { booksRepository.getBooksCount(any(), any(), any(), any(), any()) } returns 0

        // When
        val result = bookService.getAllBooksPaginated(
            page = 0,
            size = 10,
            title = null,
            author = null,
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(0, result.totalPages)
        assertEquals(0, result.totalElements)
    }
}