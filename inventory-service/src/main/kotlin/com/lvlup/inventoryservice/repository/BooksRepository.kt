package com.lvlup.inventoryservice.repository

import com.lvlup.inventoryservice.model.Book
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

@Repository
interface BooksRepository : JpaRepository<Book, Long> {
    @Modifying
    @Query("UPDATE Book b SET b.deleted = true WHERE b.id = :id")
    fun softDeleteByIdReturningCount(id: Long): Int

    @Query(
        """
      SELECT b FROM Book b
        LEFT JOIN FETCH b.category
    WHERE (COALESCE(:title, '') = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%')))
    AND (COALESCE(:author, '') = '' OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%')))
    AND (:categoryId IS NULL OR b.category.id = :categoryId)
    AND (:minPrice IS NULL OR b.price >= :minPrice)
    AND (:maxPrice IS NULL OR b.price <= :maxPrice)
"""
    )
    fun findAllPaginated(
        title: String?,
        author: String?,
        categoryId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        pageable: Pageable
    ): Page<Book>


    fun countByCategoryId(categoryId: Long): Long

    @Modifying
    @Query(
        """
        UPDATE Book b
        SET b.stock = b.stock - :quantity,
            b.updatedAt = :updatedAt
        WHERE b.id = :bookId
          AND b.stock >= :quantity
        """
    )
    fun decrementStock(
        bookId: Long,
        quantity: Int,
        updatedAt: LocalDateTime = LocalDateTime.now()
    ): Int

}