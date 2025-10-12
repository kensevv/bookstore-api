package com.lvlup.backend.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.lvlup.backend.TestFixtures
import com.lvlup.backend.dto.LoginRequest
import com.lvlup.backend.model.UserRole
import com.lvlup.backend.repository.CategoriesRepository
import com.lvlup.backend.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@AutoConfigureMockMvc
class SecurityIntegrationTest(
    @Autowired var mockMvc: MockMvc,
) : BaseIntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var categoryRepository: CategoriesRepository

    @Test
    fun `protected endpoints should require authentication`() {
        // When/Then - Cart requires auth
        mockMvc.perform(get("/api/shopping-cart"))
            .andExpect(status().isForbidden)

        // When/Then - Profile requires auth
        mockMvc.perform(get("/api/profile"))
            .andExpect(status().isForbidden)

        // When/Then - Orders require auth
        mockMvc.perform(get("/api/orders/my-orders"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `admin endpoints should require ADMIN role`() {
        // Given - Regular user
        val user = userRepository.createUser(TestFixtures.createUserFixture(role = UserRole.ROLE_USER))
        val userToken = authenticateAndGetToken(user.email, "TestPass123!")

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("testuser", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))


        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val createBookRequest = TestFixtures.createBookRequest(
            title = "Test Book",
            author = "Test Author",
            description = "Test",
            price = BigDecimal("19.99"),
            stock = 10,
            categoryId = category.id!!,
            coverImageUrl = null
        )

        // When/Then - User cannot create book
        mockMvc.perform(
            post("/api/books")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBookRequest))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `admin user should be able to create books`() {
        // Given
        val admin = userRepository.createUser(TestFixtures.createAdminFixture())
        val adminToken = authenticateAndGetToken(admin.email, "Admin123!")

        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("testuser", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))

        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture())
        val createBookRequest = TestFixtures.createBookRequest(
            title = "Test Book",
            author = "Test Author",
            description = "Test",
            price = BigDecimal("19.99"),
            stock = 10,
            categoryId = category.id!!,
            coverImageUrl = null
        )

        // When/Then
        mockMvc.perform(
            post("/api/books")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createBookRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.title").value("Test Book"))
    }

    @Test
    fun `regular user cannot create categories`() {
        // Given
        val user = userRepository.createUser(TestFixtures.createUserFixture(role = UserRole.ROLE_USER))
        val userToken = authenticateAndGetToken(user.email, "TestPass123!")

        val createCategoryRequest = TestFixtures.createCategoryRequest(
            name = "New Category",
            description = "Description"
        )

        // When/Then
        mockMvc.perform(
            post("/api/categories")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCategoryRequest))
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `admin can create categories`() {
        // Given
        val admin = userRepository.createUser(TestFixtures.createAdminFixture())
        val adminToken = authenticateAndGetToken(admin.email, "Admin123!")

        val createCategoryRequest = TestFixtures.createCategoryRequest(
            name = "New Category",
            description = "Description"
        )

        // When/Then
        mockMvc.perform(
            post("/api/categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createCategoryRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.name").value("New Category"))
    }

    @Test
    fun `user can only view their own orders`() {
        // Given
        val user1 = userRepository.createUser(TestFixtures.createUserFixture(email = "user1@example.com"))
        val user2 = userRepository.createUser(TestFixtures.createUserFixture(email = "user2@example.com"))

        val token1 = authenticateAndGetToken(user1.email, "TestPass123!")
        val token2 = authenticateAndGetToken(user2.email, "TestPass123!")

        // When/Then - User 1 can view their own orders
        mockMvc.perform(
            get("/api/orders/my-orders")
                .header("Authorization", "Bearer $token1")
        )
            .andExpect(status().isOk)

        // When/Then - User 2 can view their own orders
        mockMvc.perform(
            get("/api/orders/my-orders")
                .header("Authorization", "Bearer $token2")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `only admin can view all orders`() {
        // Given
        val user = userRepository.createUser(TestFixtures.createUserFixture(role = UserRole.ROLE_USER))
        val admin = userRepository.createUser(TestFixtures.createAdminFixture())

        val userToken = authenticateAndGetToken(user.email, "TestPass123!")
        val adminToken = authenticateAndGetToken(admin.email, "Admin123!")

        // When/Then - User cannot access all orders
        mockMvc.perform(
            get("/api/orders")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isForbidden)

        // When/Then - Admin can access all orders
        mockMvc.perform(
            get("/api/orders")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `expired token should be rejected`() {
        val invalidToken =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE1MTYyMzkwMjJ9.invalid"

        // When/Then
        mockMvc.perform(
            get("/api/profile")
                .header("Authorization", "Bearer $invalidToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `malformed token should be rejected`() {
        // Given
        val malformedToken = "not.a.valid.jwt.token"

        // When/Then
        mockMvc.perform(
            get("/api/profile")
                .header("Authorization", "Bearer $malformedToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `missing Bearer prefix should be rejected`() {
        // Given
        val user = userRepository.createUser(TestFixtures.createUserFixture())
        val token = authenticateAndGetToken(user.email, "TestPass123!")

        // When/Then - Token without "Bearer " prefix
        mockMvc.perform(
            get("/api/profile")
                .header("Authorization", token)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `authenticated user can access their cart`() {
        // Given
        val user = userRepository.createUser(TestFixtures.createUserFixture())
        val token = authenticateAndGetToken(user.email, "TestPass123!")

        // When/Then
        mockMvc.perform(
            get("/api/shopping-cart")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items").isArray)
    }

    @Test
    fun `user cannot delete books`() {
        // Given
        val user = userRepository.createUser(TestFixtures.createUserFixture(role = UserRole.ROLE_USER))
        val userToken = authenticateAndGetToken(user.email, "TestPass123!")

        // When/Then
        mockMvc.perform(
            delete("/api/books/1")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `user cannot delete categories`() {
        // Given
        val user = userRepository.createUser(TestFixtures.createUserFixture(role = UserRole.ROLE_USER))
        val userToken = authenticateAndGetToken(user.email, "TestPass123!")

        // When/Then
        mockMvc.perform(
            delete("/api/categories/delete/1")
                .header("Authorization", "Bearer $userToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `user cannot update order status`() {
        // Given
        val user = userRepository.createUser(TestFixtures.createUserFixture(role = UserRole.ROLE_USER))
        val userToken = authenticateAndGetToken(user.email, "TestPass123!")

        // When/Then
        mockMvc.perform(
            put("/api/orders/1/change-status?=newStatus=SHIPPED")
                .header("Authorization", "Bearer $userToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `admin can update order status`() {
        // Given
        val admin = userRepository.createUser(TestFixtures.createAdminFixture())
        val adminToken = authenticateAndGetToken(admin.email, "Admin123!")

        mockMvc.perform(
            put("/api/orders/999/change-status?=newStatus=SHIPPED")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound) // Not forbidden - authorization passed
    }

    // Helper method
    private fun authenticateAndGetToken(email: String, password: String): String {
        val loginRequest = LoginRequest(email = email, password = password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val response = objectMapper.readTree(result.response.contentAsString)
        return response.get("data").get("token").asText()
    }
}
