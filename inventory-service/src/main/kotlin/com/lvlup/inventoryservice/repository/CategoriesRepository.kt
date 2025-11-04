package com.lvlup.inventoryservice.repository

import com.lvlup.inventoryservice.model.Category
import com.lvlup.bookstore.jooq.tables.Categories.Companion.CATEGORIES
import com.lvlup.bookstore.jooq.tables.records.CategoriesRecord
import org.jooq.DSLContext
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface CategoriesRepository : JpaRepository<Category, Long> {

    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(name: String, id: Long): Boolean

    override fun findAll(): List<Category>
}