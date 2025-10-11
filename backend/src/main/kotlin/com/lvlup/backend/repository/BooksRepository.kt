package com.lvlup.backend.repository

import com.lvlup.backend.model.Book
import com.lvlup.backend.model.Category
import com.lvlup.bookstore.jooq.tables.Books.Companion.BOOKS
import com.lvlup.bookstore.jooq.tables.Categories.Companion.CATEGORIES
import com.lvlup.bookstore.jooq.tables.records.BooksRecord
import com.lvlup.bookstore.jooq.tables.records.CategoriesRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.jvm.java

@Repository
class BooksRepository(private val dsl: DSLContext) {

    private fun getBookSelectConditionStep() = dsl.select(
        BOOKS.asterisk(),
        CATEGORIES.asterisk()
    ).from(BOOKS)
        .leftJoin(CATEGORIES)
        .on(BOOKS.CATEGORY_ID.eq(CATEGORIES.ID))
        .where(BOOKS.DELETED.isFalse)

    fun findBookById(id: Long): Book? {
        return with(BOOKS) {
            getBookSelectConditionStep()
                .and(ID.eq(id))
                .fetchOne()?.let { result ->
                    mapRecordToBookModel(result)
                }
        }
    }

    fun findBooksPaginated(
        page: Int,
        size: Int,
        title: String?,
        author: String?,
        categoryId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?
    ): List<Book> {
        val offset = page * size
        val conditions = buildBooksSearchCondition(title, author, categoryId, minPrice, maxPrice)

        return getBookSelectConditionStep()
            .and(conditions)
            .orderBy(BOOKS.CREATED_AT.desc())
            .limit(size)
            .offset(offset)
            .fetch()
            .map { mapRecordToBookModel(it) }
    }

    fun getBooksCount(
        title: String? = null,
        author: String? = null,
        categoryId: Long? = null,
        minPrice: BigDecimal? = null,
        maxPrice: BigDecimal? = null
    ): Int {
        val conditions = buildBooksSearchCondition(title, author, categoryId, minPrice, maxPrice)
        return dsl.fetchCount(BOOKS, conditions)
    }

    private fun buildBooksSearchCondition(
        title: String?,
        author: String?,
        categoryId: Long?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?
    ): Condition {
        var condition: Condition = BOOKS.DELETED.isFalse

        title?.let {
            condition = condition.and(BOOKS.TITLE.likeIgnoreCase("%$it%"))
        }

        author?.let {
            condition = condition.and(BOOKS.AUTHOR.likeIgnoreCase("%$it%"))
        }

        categoryId?.let {
            condition = condition.and(BOOKS.CATEGORY_ID.eq(it))
        }

        minPrice?.let {
            condition = condition.and(BOOKS.PRICE.ge(it))
        }

        maxPrice?.let {
            condition = condition.and(BOOKS.PRICE.le(it))
        }

        return condition
    }

    @Transactional
    fun createBook(book: Book): Book {
        return dsl.newRecord(BOOKS).apply {
            this.title = book.title
            this.author = book.author
            this.description = book.description
            this.price = book.price
            this.stock = book.stock
            this.categoryId = book.category.id
            this.coverImageUrl = book.coverImageUrl
            this.deleted = book.deleted
            this.createdAt = book.createdAt
            this.updatedAt = book.updatedAt
        }.let { newCategoryRecord ->
            newCategoryRecord.store()
            newCategoryRecord.mapToBook(book.category)
        }
    }

    @Transactional
    fun updateBook(book: Book): Book? {
        requireNotNull(book.id) { "Book ID must not be null for update" }

        return dsl.update(BOOKS)
            .set(BOOKS.TITLE, book.title)
            .set(BOOKS.AUTHOR, book.author)
            .set(BOOKS.DESCRIPTION, book.description)
            .set(BOOKS.PRICE, book.price)
            .set(BOOKS.STOCK, book.stock)
            .set(BOOKS.CATEGORY_ID, book.category.id)
            .set(BOOKS.COVER_IMAGE_URL, book.coverImageUrl)
            .set(BOOKS.UPDATED_AT, LocalDateTime.now())
            .where(BOOKS.ID.eq(book.id))
            .returning().fetchOne()?.mapToBook(book.category)
    }

    @Transactional
    fun deleteBookById(id: Long) {
        dsl.update(BOOKS)
            .set(BOOKS.DELETED, true)
            .set(BOOKS.UPDATED_AT, LocalDateTime.now())
            .where(BOOKS.ID.eq(id))
            .execute()
    }

    private fun mapRecordToBookModel(
        resultRecord: Record,
    ): Book {
        val extractedBookRecord = resultRecord.into(BooksRecord::class.java)
        val extractedCategoryRecord = resultRecord.into(CategoriesRecord::class.java)
        return extractedBookRecord.mapToBook(
            Category(
                extractedCategoryRecord.id,
                extractedCategoryRecord.name!!,
                extractedCategoryRecord.description,
                extractedCategoryRecord.createdAt!!,
                extractedCategoryRecord.updatedAt!!,
            )
        )
    }

    private fun BooksRecord.mapToBook(category: Category): Book {
        return Book(
            id = id,
            title = title!!,
            author = author!!,
            description = description,
            price = price!!,
            stock = stock!!,
            category = category,
            coverImageUrl = coverImageUrl,
            createdAt = createdAt!!,
            updatedAt = updatedAt!!,
            deleted = deleted!!
        )
    }
}