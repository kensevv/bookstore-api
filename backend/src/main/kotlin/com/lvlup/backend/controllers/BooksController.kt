package com.lvlup.backend.controllers

import com.lvlup.backend.dto.ApiResponseFactory
import com.lvlup.backend.dto.PaginatedDataResponse
import com.lvlup.backend.dto.BookRequest
import com.lvlup.backend.dto.SuccessResponse
import com.lvlup.backend.model.Book
import com.lvlup.backend.service.BookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

@RestController
@RequestMapping("/api/books")
class BooksController(
    private val bookService: BookService
) {

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createBook(@Valid @RequestBody request: BookRequest): ResponseEntity<SuccessResponse<Book>> {
        val response = bookService.createBook(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseFactory.success(response))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateBook(
        @PathVariable id: Long,
        @Valid @RequestBody request: BookRequest
    ): ResponseEntity<SuccessResponse<Book>> {
        val response = bookService.updateBook(id, request)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteBook(@PathVariable id: Long): ResponseEntity<SuccessResponse<Unit>> {
        bookService.deleteBook(id)
        return ResponseEntity.ok(ApiResponseFactory.success(message = "Book deleted successfully"))
    }

    @GetMapping("/{id}")
    fun getBookById(@PathVariable id: Long): ResponseEntity<SuccessResponse<Book>> {
        val response = bookService.getBookById(id)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @GetMapping
    fun getAllBooks(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) author: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?
    ): ResponseEntity<SuccessResponse<PaginatedDataResponse<Book>>> {
        val response = bookService.getAllBooksPaginated(page, size, title, author, categoryId, minPrice, maxPrice)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }
}