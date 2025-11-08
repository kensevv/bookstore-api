package com.lvlup.inventoryservice.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal
import java.time.LocalDateTime


@Entity
@Table(
    name = "books",
    indexes = [
        Index(name = "idx_books_title", columnList = "title"),
        Index(name = "idx_books_author", columnList = "author")
    ]
)
@SQLDelete(sql = "UPDATE books SET deleted = true WHERE id = ?")
@SQLRestriction(value = "deleted = false")
data class Book(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "title", nullable = false, length = 255)
    val title: String,

    @Column(name = "author", nullable = false, length = 255)
    val author: String,

    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    val price: BigDecimal,

    @Column(name = "stock", nullable = false)
    val stock: Int,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = ForeignKey(name = "fk_books_category"))
    val category: Category,

    @Column(name = "cover_image_url", length = 1000)
    val coverImageUrl: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "deleted", nullable = false)
    val deleted: Boolean = false
)