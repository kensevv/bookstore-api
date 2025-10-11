package com.lvlup.backend.model

import java.time.LocalDateTime

enum class UserRole {
    ROLE_ADMIN, ROLE_USER
}

data class User(
    val email: String,
    val firstName: String,
    val lastName: String,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deleted: Boolean = false
)