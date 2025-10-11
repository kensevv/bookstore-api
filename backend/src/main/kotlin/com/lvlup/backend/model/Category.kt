package com.lvlup.backend.model

import java.time.LocalDateTime

data class Category(
    val id: Long? = null,
    val name: String,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)