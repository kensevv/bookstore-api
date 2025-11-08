package com.lvlup.inventoryservice.repository

import com.lvlup.inventoryservice.model.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CategoriesRepository : JpaRepository<Category, Long> {

    fun existsByName(name: String): Boolean

    fun existsByNameAndIdNot(name: String, id: Long): Boolean

    override fun findAll(): List<Category>
}