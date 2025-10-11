package com.lvlup.backend.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CategoryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name must not exceed 100 characters")
    val name: String,

    @field:Size(max = 1000, message = "Description must not exceed 1000 characters")
    val description: String?
)
