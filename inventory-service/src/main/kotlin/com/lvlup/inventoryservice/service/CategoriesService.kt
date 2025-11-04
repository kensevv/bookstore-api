package com.lvlup.inventoryservice.service

import com.lvlup.inventoryservice.dto.CategoryRequest
import com.lvlup.inventoryservice.exception.CategoryNotFoundException
import com.lvlup.inventoryservice.exception.InvalidOperationException
import com.lvlup.inventoryservice.model.Category
import com.lvlup.inventoryservice.redis.RedisCacheNames
import com.lvlup.inventoryservice.repository.BooksRepository
import com.lvlup.inventoryservice.repository.CategoriesRepository
import exception.DuplicateResourceException
import mu.KotlinLogging
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
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
    @Cacheable(value = [RedisCacheNames.CATEGORY], key = "#id")
    fun getCategoryById(id: Long): Category {
        logger.debug("Fetching category with ID: $id")

        val category = categoriesRepository.findById(id).orElseThrow {
            throw CategoryNotFoundException("Category not found with ID: $id")
        }

        return category
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [RedisCacheNames.CATEGORIES])
    fun getAllCategories(): List<Category> {
        logger.debug("Fetching all categories")
        return categoriesRepository.findAll()
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CachePut(value = [RedisCacheNames.CATEGORY], key = "#result.id")
    @CacheEvict(value = [RedisCacheNames.CATEGORIES], allEntries = true)
    fun createCategory(createRequest: CategoryRequest): Category {
        logger.info("Creating new category: ${createRequest.name}")

        if (categoriesRepository.existsByName(createRequest.name)) {
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

        val createdCategory = categoriesRepository.save(category)
        logger.info("Category created successfully with ID: ${createdCategory.id}")

        return createdCategory
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(
        put = [
            CachePut(value = [RedisCacheNames.CATEGORY], key = "#result.id"),
        ],
        evict = [
            CacheEvict(
                value = [RedisCacheNames.BOOKS, RedisCacheNames.BOOK, RedisCacheNames.CATEGORIES],
                allEntries = true
            ),
        ]
    )
    fun updateCategory(id: Long, updateRequest: CategoryRequest): Category {
        logger.info("Updating category with ID: $id")

        val category = categoriesRepository.findById(id).orElseThrow {
            CategoryNotFoundException("Category not found with ID: $id")
        }

        if (categoriesRepository.existsByNameAndIdNot(updateRequest.name, id)) {
            logger.warn("Category update failed: Name already exists - ${updateRequest.name}")
            throw DuplicateResourceException("Category with name '${updateRequest.name}' already exists")
        }

        category.apply {
            name = updateRequest.name.trim()
            description = updateRequest.description?.trim()
            updatedAt = LocalDateTime.now()
        }

        val savedCategory = categoriesRepository.save(category)
        logger.info("Category updated successfully with ID: $id")

        return savedCategory
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @Caching(
        evict = [
            CacheEvict(value = [RedisCacheNames.CATEGORY], key = "#categoryId"),
            CacheEvict(value = [RedisCacheNames.CATEGORIES], allEntries = true),
        ]
    )
    fun deleteCategory(categoryId: Long): Category {
        logger.info("Attempting to delete category with ID: $categoryId")

        val category = categoriesRepository.findById(categoryId).orElseThrow {
            CategoryNotFoundException("Category not found with ID: $categoryId")
        }

        val bookCount = booksRepository.getBooksCount(categoryId = categoryId)

        if (bookCount > 0) {
            logger.warn("Category deletion failed: Category ${category.name} has $bookCount associated books")
            throw InvalidOperationException(
                "Cannot delete category ${category.name} (ID: $categoryId). It has $bookCount associated book(s). " +
                        "Please reassign or delete the books first."
            )
        }

        categoriesRepository.deleteById(categoryId)
        logger.info("Category deleted successfully with ID: $categoryId")
        return category
    }

}