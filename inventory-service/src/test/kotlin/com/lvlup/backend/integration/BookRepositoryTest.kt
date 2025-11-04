package com.lvlup.inventoryservice.integration

import com.lvlup.inventoryservice.TestFixtures
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.CategoriesRepository
import com.lvlup.bookstore.jooq.tables.Books.Companion.BOOKS
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.math.BigDecimal

class BookRepositoryIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var bookRepository: BooksRepository

    @Autowired
    private lateinit var categoryRepository: CategoriesRepository

    @BeforeEach
    fun setupAuth() {
        val auth =
            UsernamePasswordAuthenticationToken("testuser", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        SecurityContextHolder.getContext().authentication = auth
    }

    @Test
    fun `should save and retrieve book`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val book = TestFixtures.createBookFixture(categoryId = category.id!!)

        // When
        val savedBook = bookRepository.createBook(book)
        val retrievedBook = bookRepository.findBookById(savedBook.id!!)

        // Then
        assertNotNull(retrievedBook)
        assertEquals(book.title, retrievedBook!!.title)
        assertEquals(book.author, retrievedBook.author)
        assertEquals(book.price, retrievedBook.price)
        assertEquals(book.stock, retrievedBook.stock)
        assertEquals(category.id, retrievedBook.category.id)
    }

    @Test
    fun `should find books by title with case-insensitive search`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        bookRepository.createBook(TestFixtures.createBookFixture(title = "Clean Code", categoryId = category.id!!))
        bookRepository.createBook(TestFixtures.createBookFixture(title = "The Clean Coder", categoryId = category.id!!))
        bookRepository.createBook(TestFixtures.createBookFixture(title = "Design Patterns", categoryId = category.id!!))

        // When
        val results = bookRepository.findBooksPaginated(
            page = 0,
            size = 10,
            title = "clean",
            author = null,
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { it.title.contains("Clean", ignoreCase = true) })
    }

    @Test
    fun `should find books by author`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        bookRepository.createBook(TestFixtures.createBookFixture(author = "Robert Martin", categoryId = category.id!!))
        bookRepository.createBook(TestFixtures.createBookFixture(author = "Martin Fowler", categoryId = category.id!!))
        bookRepository.createBook(TestFixtures.createBookFixture(author = "Kent Beck", categoryId = category.id!!))

        // When
        val results = bookRepository.findBooksPaginated(
            page = 0,
            size = 10,
            title = null,
            author = "martin",
            categoryId = null,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { it.author.contains("Martin", ignoreCase = true) })
    }

    @Test
    fun `should filter books by price range`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        bookRepository.createBook(
            TestFixtures.createBookFixture(
                price = BigDecimal("10.00"),
                categoryId = category.id!!
            )
        )
        bookRepository.createBook(
            TestFixtures.createBookFixture(
                price = BigDecimal("25.00"),
                categoryId = category.id!!
            )
        )
        bookRepository.createBook(
            TestFixtures.createBookFixture(
                price = BigDecimal("50.00"),
                categoryId = category.id!!
            )
        )

        // When
        val results = bookRepository.findBooksPaginated(
            page = 0,
            size = 10,
            title = null,
            author = null,
            categoryId = null,
            minPrice = BigDecimal("20.00"),
            maxPrice = BigDecimal("40.00")
        )

        // Then
        assertEquals(1, results.size)
        assertEquals(BigDecimal("25.00"), results[0].price)
    }

    @Test
    fun `should filter books by category`() {
        // Given
        val category1 = categoryRepository.createCategory(TestFixtures.createCategoryFixture(name = "Fiction"))
        val category2 = categoryRepository.createCategory(TestFixtures.createCategoryFixture(name = "Science"))

        bookRepository.createBook(TestFixtures.createBookFixture(title = "Book 1", categoryId = category1.id!!))
        bookRepository.createBook(TestFixtures.createBookFixture(title = "Book 2", categoryId = category1.id!!))
        bookRepository.createBook(TestFixtures.createBookFixture(title = "Book 3", categoryId = category2.id!!))

        // When
        val results = bookRepository.findBooksPaginated(
            page = 0,
            size = 10,
            title = null,
            author = null,
            categoryId = category1.id,
            minPrice = null,
            maxPrice = null
        )

        // Then
        assertEquals(2, results.size)
        assertTrue(results.all { it.category.id == category1.id })
    }

    @Test
    fun `should paginate books correctly`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        repeat(25) { i ->
            bookRepository.createBook(
                TestFixtures.createBookFixture(
                    title = "Book $i",
                    categoryId = category.id!!
                )
            )
        }

        // When - Get first page
        val page1 = bookRepository.findBooksPaginated(0, 10, null, null, null, null, null)
        val page2 = bookRepository.findBooksPaginated(1, 10, null, null, null, null, null)
        val page3 = bookRepository.findBooksPaginated(2, 10, null, null, null, null, null)

        // Then
        assertEquals(10, page1.size)
        assertEquals(10, page2.size)
        assertEquals(5, page3.size)

        // Verify no duplicates between pages
        val allIds = (page1 + page2 + page3).map { it.id }.toSet()
        assertEquals(25, allIds.size)
    }

    @Test
    fun `should count books correctly with filters`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        repeat(15) {
            bookRepository.createBook(
                TestFixtures.createBookFixture(
                    title = "Clean Code $it",
                    categoryId = category.id!!
                )
            )
        }
        repeat(10) {
            bookRepository.createBook(
                TestFixtures.createBookFixture(
                    title = "Design Patterns $it",
                    categoryId = category.id!!
                )
            )
        }

        // When
        val totalCount = bookRepository.getBooksCount(null, null, null, null, null)
        val cleanCodeCount = bookRepository.getBooksCount("clean", null, null, null, null)

        // Then
        assertEquals(25, totalCount)
        assertEquals(15, cleanCodeCount)
    }

    @Test
    fun `decrementStock should update stock atomically`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val book = bookRepository.createBook(
            TestFixtures.createBookFixture(
                stock = 10,
                categoryId = category.id!!
            )
        )

        // When
        val success = bookRepository.decrementStock(book.id!!, 5)

        // Then
        assertTrue(success)
        val updatedBook = bookRepository.findBookById(book.id!!)
        assertEquals(5, updatedBook!!.stock)
    }

    @Test
    fun `decrementStock should fail when insufficient stock`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val book = bookRepository.createBook(
            TestFixtures.createBookFixture(
                stock = 5,
                categoryId = category.id!!
            )
        )

        // When
        val success = bookRepository.decrementStock(book.id!!, 10)

        // Then
        assertFalse(success)
        val unchangedBook = bookRepository.findBookById(book.id!!)
        assertEquals(5, unchangedBook!!.stock) // Stock should remain unchanged
    }

    @Test
    fun `decrementStock should prevent negative stock`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val book = bookRepository.createBook(
            TestFixtures.createBookFixture(
                stock = 5,
                categoryId = category.id!!
            )
        )

        // When - Try to deduct exactly the stock amount
        val success1 = bookRepository.decrementStock(book.id!!, 5)
        val success2 = bookRepository.decrementStock(book.id!!, 1)

        // Then
        assertTrue(success1)
        assertFalse(success2) // Should fail because stock is now 0

        val finalBook = bookRepository.findBookById(book.id!!)
        assertEquals(0, finalBook!!.stock)
    }

    @Test
    fun `soft delete should mark book as inactive`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val book = bookRepository.createBook(TestFixtures.createBookFixture(categoryId = category.id!!))

        // When
        bookRepository.deleteBookById(book.id!!)

        // Then
        val deletedBook = bookRepository.findBookById(book.id!!)
        assertNull(deletedBook) // findById only returns active books

        // Verify book still exists in database but is inactive
        val inactiveBook = dsl.selectFrom(BOOKS)
            .where(BOOKS.ID.eq(book.id!!))
            .fetchOne()

        assertNotNull(inactiveBook)
        assertTrue(inactiveBook?.deleted == true)
    }

    @Test
    fun `findByIdWithCategory should return book with category name`() {
        // Given
        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture(name = "Science Fiction"))
        val book = bookRepository.createBook(TestFixtures.createBookFixture(categoryId = category.id!!))

        // When
        val retrievedBook = bookRepository.findBookById(book.id!!)!!

        // Then
        assertEquals(book.id, retrievedBook.id)
        assertEquals("Science Fiction", retrievedBook.category.name)
    }

    @Test
    fun `should handle multiple filters simultaneously`() {
        // Given
        val category1 = categoryRepository.createCategory(TestFixtures.createCategoryFixture(name = "Fiction"))
        val category2 = categoryRepository.createCategory(TestFixtures.createCategoryFixture(name = "Science"))

        bookRepository.createBook(
            TestFixtures.createBookFixture(
                title = "Clean Code",
                author = "Robert Martin",
                price = BigDecimal("45.00"),
                categoryId = category1.id!!
            )
        )
        bookRepository.createBook(
            TestFixtures.createBookFixture(
                title = "The Clean Coder",
                author = "Robert Martin",
                price = BigDecimal("35.00"),
                categoryId = category1.id!!
            )
        )
        bookRepository.createBook(
            TestFixtures.createBookFixture(
                title = "Clean Architecture",
                author = "Robert Martin",
                price = BigDecimal("40.00"),
                categoryId = category2.id!!
            )
        )

        // When - Search for "clean" books by "martin" in Fiction category, price 30-50
        val results = bookRepository.findBooksPaginated(
            page = 0,
            size = 10,
            title = "clean",
            author = "martin",
            categoryId = category1.id,
            minPrice = BigDecimal("30.00"),
            maxPrice = BigDecimal("50.00")
        )

        // Then
        assertEquals(2, results.size)
        assertTrue(results.all {
            it.title.contains("Clean", ignoreCase = true) &&
                    it.author.contains("Martin", ignoreCase = true) &&
                    it.category.id == category1.id &&
                    it.price >= BigDecimal("30.00") &&
                    it.price <= BigDecimal("50.00")
        })
    }
}
