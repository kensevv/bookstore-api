package com.lvlup.backend.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class BookRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String,

    @field:NotBlank(message = "Author is required")
    @field:Size(max = 255, message = "Author must not exceed 255 characters")
    val author: String,

    @field:Size(max = 5000, message = "Description must not exceed 5000 characters")
    val description: String?,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Price must be greater than or equal to 0")
    @field:DecimalMax(value = "999999.99", message = "Price must not exceed 999999.99")
    val price: BigDecimal,

    @field:NotNull(message = "Stock is required")
    @field:Min(value = 0, message = "Stock must be greater than or equal to 0")
    val stock: Int,

    @field:NotNull(message = "Category ID is required")
    @field:Positive(message = "Category ID must be positive")
    val categoryId: Long,

    @field:Size(max = 500, message = "Cover image URL must not exceed 1000 characters")
    val coverImageUrl: String?
)