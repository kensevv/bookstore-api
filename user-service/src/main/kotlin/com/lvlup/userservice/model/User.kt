package com.lvlup.userservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

enum class UserRole {
    ROLE_ADMIN, ROLE_USER
}

@Entity
@Table(name = "users")
class User(
    @Id var email: String,

    @Column(nullable = false) var firstName: String,

    @Column(nullable = false) var lastName: String,

    @Column(nullable = false) var passwordHash: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var role: UserRole,

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    var createdAt: LocalDateTime,

    @UpdateTimestamp
    @Column(nullable = false)
    var updatedAt: LocalDateTime,

    @Column(nullable = false) var deleted: Boolean = false
)