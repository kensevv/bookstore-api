package com.lvlup.backend.repository

import com.lvlup.backend.model.Category
import com.lvlup.bookstore.jooq.tables.Categories.Companion.CATEGORIES
import com.lvlup.bookstore.jooq.tables.records.CategoriesRecord
import org.jooq.DSLContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class CategoriesRepository(private val dsl: DSLContext) {

    fun findById(id: Long): Category? {
        return dsl.selectFrom(CATEGORIES)
            .where(CATEGORIES.ID.eq(id))
            .fetchOne()?.mapToCategory()
    }

    fun findAll(): List<Category> {
        return dsl.selectFrom(CATEGORIES)
            .orderBy(CATEGORIES.NAME.asc())
            .fetch()
            .map { it.mapToCategory() }
    }

    fun existsByName(name: String): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(CATEGORIES)
                .where(CATEGORIES.NAME.eq(name))
        )
    }

    fun existsByNameAndIdNot(name: String, id: Long): Boolean {
        return dsl.fetchExists(
            dsl.selectFrom(CATEGORIES)
                .where(CATEGORIES.NAME.eq(name).and(CATEGORIES.ID.ne(id)))
        )
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun createCategory(category: Category): Category {
        return dsl.newRecord(CATEGORIES).apply {
            this.name = category.name
            this.description = category.description
            this.createdAt = category.createdAt
            this.updatedAt = category.updatedAt
        }.let { newCategoryRecord ->
            newCategoryRecord.store()
            newCategoryRecord.mapToCategory()
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun updateCategory(category: Category): Category? {
        requireNotNull(category.id) { "Category ID must not be null for update" }

        return dsl.update(CATEGORIES)
            .set(CATEGORIES.NAME, category.name)
            .set(CATEGORIES.DESCRIPTION, category.description)
            .set(CATEGORIES.UPDATED_AT, LocalDateTime.now())
            .where(CATEGORIES.ID.eq(category.id))
            .returning().fetchOne()?.mapToCategory()
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteById(id: Long) {
        dsl.deleteFrom(CATEGORIES)
            .where(CATEGORIES.ID.eq(id))
            .execute()
    }

    private fun CategoriesRecord.mapToCategory(): Category {
        return Category(
            id = id!!,
            name = name!!,
            description = description,
            createdAt = createdAt!!,
            updatedAt = updatedAt!!
        )
    }
}