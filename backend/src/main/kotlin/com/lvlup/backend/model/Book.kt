package com.lvlup.backend.model

import java.math.BigDecimal
import java.time.LocalDateTime

data class Book(
    val id: Long? = null,
    val title: String,
    val author: String,
    val description: String? = null,
    val price: BigDecimal,
    val stock: Int,
    val category: Category,
    val coverImageUrl: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deleted: Boolean
)