package com.lvlup.backend

import com.lvlup.backend.dto.BookRequest
import com.lvlup.backend.dto.CategoryRequest
import com.lvlup.backend.dto.RegisterRequest
import com.lvlup.backend.model.Book
import com.lvlup.backend.model.Category
import com.lvlup.backend.model.ShoppingCart
import com.lvlup.backend.model.ShoppingCartItem
import com.lvlup.backend.model.User
import com.lvlup.backend.model.UserRole
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.math.BigDecimal
import java.time.LocalDateTime

object TestFixtures {

    private val passwordEncoder = BCryptPasswordEncoder()

    // User
    fun createUserFixture(
        email: String = "test@example.com",
        password: String = "TestPass123!",
        firstName: String = "Test",
        lastName: String = "User",
        role: UserRole = UserRole.ROLE_USER,
    ): User {
        return User(
            email = email,
            passwordHash = passwordEncoder.encode(password),
            firstName = firstName,
            lastName = lastName,
            role = role,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            deleted = false
        )
    }

    fun createAdminFixture(
        email: String = "admin@example.com",
    ): User {
        return createUserFixture(
            email = email,
            password = "Admin123!",
            firstName = "Admin",
            lastName = "User",
            role = UserRole.ROLE_ADMIN,
        )
    }

    fun createRegisterRequest(
        email: String = "newuser@example.com",
        password: String = "NewPass123!",
        firstName: String = "New",
        lastName: String = "User"
    ): RegisterRequest {
        return RegisterRequest(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
        )
    }

    // Category
    fun createCategoryFixture(
        name: String = "Test Category",
        description: String? = "Test category description",
        id: Long = 1L
    ): Category {
        return Category(
            id = id,
            name = name,
            description = description,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    fun createCategoryRequest(
        name: String = "New Category",
        description: String? = "New category description"
    ): CategoryRequest {
        return CategoryRequest(
            name = name,
            description = description
        )
    }

    // Book fixtures
    fun createBookFixture(
        id: Long = 1L,
        title: String = "Test Book",
        author: String = "Test Author",
        description: String? = "Test book description",
        price: BigDecimal = BigDecimal("19.99"),
        stock: Int = 100,
        categoryId: Long = 1L,
        coverImageUrl: String? = "https://example.com/cover.jpg",
        deleted: Boolean = false,
        createdAt: LocalDateTime = LocalDateTime.now(),
    ): Book {
        return Book(
            id = id,
            title = title,
            author = author,
            description = description,
            price = price,
            stock = stock,
            category = Category(
                id = categoryId,
                name = "haha",
                description = "haha",
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ),
            coverImageUrl = coverImageUrl,
            createdAt = createdAt,
            updatedAt = LocalDateTime.now(),
            deleted = deleted
        )
    }

    fun createBookRequest(
        title: String = "New Book",
        author: String = "New Author",
        description: String? = "New book description",
        price: BigDecimal = BigDecimal("29.99"),
        stock: Int = 50,
        categoryId: Long = 1L,
        coverImageUrl: String? = null
    ): BookRequest {
        return BookRequest(
            title = title,
            author = author,
            description = description,
            price = price,
            stock = stock,
            categoryId = categoryId,
            coverImageUrl = coverImageUrl
        )
    }

    // Cart
    fun createCartFixture(
        id: Long = 1L,
        userEmail: String = "test@example.com",
    ): ShoppingCart {
        return ShoppingCart(
            id = id,
            userEmail = userEmail,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }

    fun createCartItemFixture(
        id: Long = 1L,
        cartId: Long = 1L,
        bookId: Long = 1L,
        quantity: Int = 1,
    ): ShoppingCartItem {
        return ShoppingCartItem(
            id = id,
            cartId = cartId,
            bookId = bookId,
            quantity = quantity,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}