package com.lvlup.backend.controllers

import com.lvlup.backend.dto.ApiResponseFactory
import com.lvlup.backend.dto.CategoryRequest
import com.lvlup.backend.dto.SuccessResponse
import com.lvlup.backend.model.Category
import com.lvlup.backend.service.CategoriesService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/categories")
class CategoriesController(
    private val categoryService: CategoriesService
) {
    @GetMapping
    fun getAllCategories(): ResponseEntity<SuccessResponse<List<Category>>> {
        val response = categoryService.getAllCategories()
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @GetMapping("/{id}")
    fun getCategoryById(@PathVariable id: Long): ResponseEntity<SuccessResponse<Category>> {
        val response = categoryService.getCategoryById(id)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createCategory(@Valid @RequestBody createRequest: CategoryRequest): ResponseEntity<SuccessResponse<Category>> {
        val response = categoryService.createCategory(createRequest)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseFactory.success(response))
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody updateRequest: CategoryRequest
    ): ResponseEntity<SuccessResponse<Category>> {
        val response = categoryService.updateCategory(id, updateRequest)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteCategory(@PathVariable id: Long): ResponseEntity<SuccessResponse<Unit>> {
        val response = categoryService.deleteCategory(id)
        return ResponseEntity.ok(ApiResponseFactory.success(message = "Category ${response.name} deleted successfully"))
    }

}