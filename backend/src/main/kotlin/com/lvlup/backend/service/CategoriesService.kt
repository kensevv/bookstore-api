package com.lvlup.backend.service

import com.lvlup.backend.dto.CategoryRequest
import com.lvlup.backend.exception.CategoryNotFoundException
import com.lvlup.backend.exception.DuplicateResourceException
import com.lvlup.backend.exception.InvalidOperationException
import com.lvlup.backend.model.Category
import com.lvlup.backend.repository.BooksRepository
import com.lvlup.backend.repository.CategoriesRepository
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CategoriesService(
    private val categoriesRepository: CategoriesRepository,
    private val booksRepository: BooksRepository,
) {

    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun getCategoryById(id: Long): Category {
        logger.debug("Fetching category with ID: $id")

        val category = categoriesRepository.findCategoryById(id)
            ?: throw CategoryNotFoundException("Category not found with ID: $id")

        return category
    }

    @Transactional(readOnly = true)
    fun getAllCategories(): List<Category> {
        logger.debug("Fetching all categories")
        return categoriesRepository.findAllCategories()
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun createCategory(createRequest: CategoryRequest): Category {
        logger.info("Creating new category: ${createRequest.name}")

        if (categoriesRepository.existsCategoryByName(createRequest.name)) {
            logger.warn("Category creation failed: Name already exists - ${createRequest.name}")
            throw DuplicateResourceException("Category with name '${createRequest.name}' already exists")
        }

        val createdTimestamp = LocalDateTime.now()
        val category = Category(
            name = createRequest.name.trim(),
            description = createRequest.description?.trim(),
            createdAt = createdTimestamp,
            updatedAt = createdTimestamp
        )

        val createdCategory = categoriesRepository.createCategory(category)
        logger.info("Category created successfully with ID: ${createdCategory.id}")

        return createdCategory
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun updateCategory(id: Long, updateRequest: CategoryRequest): Category {
        logger.info("Updating category with ID: $id")

        val existingCategory = categoriesRepository.findCategoryById(id)
            ?: throw CategoryNotFoundException("Category not found with ID: $id")

        if (categoriesRepository.existsCategoryByNameAndIdNot(updateRequest.name, id)) {
            logger.warn("Category update failed: Name already exists - ${updateRequest.name}")
            throw DuplicateResourceException("Category with name '${updateRequest.name}' already exists")
        }

        val updatedCategory = existingCategory.copy(
            name = updateRequest.name.trim(),
            description = updateRequest.description?.trim(),
            updatedAt = LocalDateTime.now()
        )

        val savedCategory = categoriesRepository.updateCategory(updatedCategory)
            ?: throw CategoryNotFoundException("Category not found with ID: $id")

        logger.info("Category updated successfully with ID: $id")

        return savedCategory
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteCategory(categoryId: Long): Category {
        logger.info("Attempting to delete category with ID: $categoryId")

        val category = categoriesRepository.findCategoryById(categoryId)
            ?: throw CategoryNotFoundException("Category not found with ID: $categoryId")

        val bookCount = booksRepository.getBooksCount(categoryId = categoryId)

        if (bookCount > 0) {
            logger.warn("Category deletion failed: Category ${category.name} has $bookCount associated books")
            throw InvalidOperationException(
                "Cannot delete category ${category.name} (ID: $categoryId). It has $bookCount associated book(s). " +
                        "Please reassign or delete the books first."
            )
        }

        categoriesRepository.deleteCategoryById(categoryId)
        logger.info("Category deleted successfully with ID: $categoryId")
        return category
    }

}