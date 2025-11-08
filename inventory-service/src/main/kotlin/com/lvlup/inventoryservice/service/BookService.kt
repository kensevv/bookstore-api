package com.lvlup.inventoryservice.service

import com.lvlup.inventoryservice.dto.BookRequest
import com.lvlup.inventoryservice.exception.BookNotFoundException
import com.lvlup.inventoryservice.exception.CategoryNotFoundException
import com.lvlup.inventoryservice.model.Book
import com.lvlup.inventoryservice.redis.RedisCacheNames
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.CategoriesRepository
import dto.PaginatedDataResponse
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BookService(
    private val bookRepository: BooksRepository,
    private val categoryRepository: CategoriesRepository,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    @CacheEvict(value = [RedisCacheNames.BOOKS], allEntries = true)
    @CachePut(value = [RedisCacheNames.BOOK], key = "#result.id")
    fun createBook(request: BookRequest): Book {
        logger.info("Creating new book: ${request.title}")

        val selectedCategory = categoryRepository.findById(request.categoryId).orElseThrow {
            CategoryNotFoundException("Category not found with ID: ${request.categoryId}")
        }

        val createdTimestamp = LocalDateTime.now()
        val book = Book(
            title = request.title.trim(),
            author = request.author.trim(),
            description = request.description?.trim(),
            price = request.price,
            stock = request.stock,
            category = selectedCategory,
            coverImageUrl = request.coverImageUrl?.trim(),
            createdAt = createdTimestamp,
            updatedAt = createdTimestamp,
        )

        val savedBook = bookRepository.save(book)
        logger.info("Book created successfully with ID: ${savedBook.id}")

        return savedBook
    }

    @Transactional
    @Caching(
        put = [
            CachePut(value = [RedisCacheNames.BOOK], key = "#result.id"),
        ],
        evict = [
            CacheEvict(value = [RedisCacheNames.BOOKS], allEntries = true)
        ]
    )
    fun updateBook(id: Long, request: BookRequest): Book {
        logger.info("Updating book with ID: $id")

        val existingBook = bookRepository.findById(id).orElseThrow {
            BookNotFoundException("Book not found with ID: $id")
        }

        val selectedCategory = categoryRepository.findById(request.categoryId).orElseThrow {
            CategoryNotFoundException("Category not found with ID: ${request.categoryId}")
        }

        val updatedBook = existingBook.copy(
            title = request.title.trim(),
            author = request.author.trim(),
            description = request.description?.trim(),
            price = request.price,
            stock = request.stock,
            category = selectedCategory,
            coverImageUrl = request.coverImageUrl?.trim()
        )

        val savedBook = bookRepository.saveAndFlush(updatedBook)

        logger.info("Book updated successfully with ID: ${savedBook.id}")

        return savedBook
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [RedisCacheNames.BOOK], key = "#id"),
            CacheEvict(value = [RedisCacheNames.BOOKS], allEntries = true)
        ]
    )
    fun deleteBook(id: Long) {
        logger.info("Deleting book with ID: $id")
        val deleted = bookRepository.softDeleteByIdReturningCount(id)
        if (deleted == 0) throw BookNotFoundException("Book not found with ID: $id")
        logger.info("Book deleted successfully with ID: $id")
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [RedisCacheNames.BOOK], key = "#id")
    fun getBookById(id: Long): Book {
        logger.debug("Fetching book with ID: $id")

        return bookRepository.findById(id).orElseThrow {
            BookNotFoundException("Book not found with ID: $id")
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = [RedisCacheNames.BOOKS],
        key = "T(String).format('%d_%d_%s_%s_%s_%s_%s', #page, #size, #title, #author, #categoryId, #minPrice, #maxPrice)",
    )
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

        val pageResult = bookRepository.findAllPaginated(
            title?.trim(),
            author?.trim(),
            categoryId,
            minPrice,
            maxPrice,
            PageRequest.of(
                maxOf(0, page),
                minOf(maxOf(1, size), 100),
                sortByKProperty(Book::title)
            )
        )

        return pageResult.toPaginatedDataResponse()

    }
}