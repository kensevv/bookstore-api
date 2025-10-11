package com.lvlup.backend.service

import com.lvlup.backend.dto.PaginatedDataResponse
import com.lvlup.backend.dto.BookRequest
import com.lvlup.backend.exception.BookNotFoundException
import com.lvlup.backend.exception.CategoryNotFoundException
import com.lvlup.backend.model.Book
import com.lvlup.backend.repository.BooksRepository
import com.lvlup.backend.repository.CategoriesRepository
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BookService(
    private val bookRepository: BooksRepository,
    private val categoryRepository: CategoriesRepository
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun createBook(request: BookRequest): Book {
        logger.info("Creating new book: ${request.title}")

        val selectedCategory = categoryRepository.findCategoryById(request.categoryId)
            ?: throw CategoryNotFoundException("Category not found with ID: ${request.categoryId}")

        val createdTimestamp = LocalDateTime.now()
        val book = Book(
            id = null,
            title = request.title.trim(),
            author = request.author.trim(),
            description = request.description?.trim(),
            price = request.price,
            stock = request.stock,
            category = selectedCategory,
            coverImageUrl = request.coverImageUrl?.trim(),
            createdAt = createdTimestamp,
            updatedAt = createdTimestamp,
            deleted = false,
        )

        val savedBook = bookRepository.createBook(book)
        logger.info("Book created successfully with ID: ${savedBook.id}")

        return savedBook
    }

    @Transactional
    fun updateBook(id: Long, request: BookRequest): Book {
        logger.info("Updating book with ID: $id")

        val existingBook = bookRepository.findBookById(id)
            ?: throw BookNotFoundException("Book not found with ID: $id")

        val selectedCategory = categoryRepository.findCategoryById(request.categoryId)
            ?: throw CategoryNotFoundException("Category not found with ID: ${request.categoryId}")

        val updatedBook = existingBook.copy(
            title = request.title.trim(),
            author = request.author.trim(),
            description = request.description?.trim(),
            price = request.price,
            stock = request.stock,
            category = selectedCategory,
            coverImageUrl = request.coverImageUrl?.trim()
        )

        val savedBook = bookRepository.updateBook(updatedBook)
            ?: throw BookNotFoundException("Book not found with ID: $id")

        logger.info("Book updated successfully with ID: $id")

        return savedBook
    }

    @Transactional
    fun deleteBook(id: Long) {
        logger.info("Deleting book with ID: $id")

        bookRepository.findBookById(id)
            ?: throw BookNotFoundException("Book not found with ID: $id")

        bookRepository.deleteBookById(id)
        logger.info("Book deleted successfully with ID: $id")
    }

    @Transactional(readOnly = true)
    fun getBookById(id: Long): Book {
        logger.debug("Fetching book with ID: $id")

        val book = bookRepository.findBookById(id)
            ?: throw BookNotFoundException("Book not found with ID: $id")

        return book
    }

    @Transactional(readOnly = true)
    fun getAllBooksPaginated(
        page: Int,
        size: Int,
        title: String?,
        author: String?,
        categoryId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?
    ): PaginatedDataResponse<Book> {
        logger.debug("Fetching books - page: $page, size: $size")

        val validatedPage = maxOf(0, page)
        val validatedSize = minOf(maxOf(1, size), 100)

        val books = bookRepository.findBooksPaginated(
            validatedPage,
            validatedSize,
            title?.trim(),
            author?.trim(),
            categoryId,
            minPrice,
            maxPrice
        )

        val totalElements = bookRepository.getBooksCount(
            title?.trim(),
            author?.trim(),
            categoryId,
            minPrice,
            maxPrice
        )

        val totalPages = if (totalElements == 0) 0 else (totalElements + validatedSize - 1) / validatedSize

        return PaginatedDataResponse(
            data = books,
            totalElements = totalElements,
            totalPages = totalPages,
            currentPage = validatedPage,
            pageSize = validatedSize
        )
    }
}