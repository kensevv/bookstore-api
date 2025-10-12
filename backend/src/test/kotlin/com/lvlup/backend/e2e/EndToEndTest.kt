package com.lvlup.backend.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.lvlup.backend.TestFixtures
import com.lvlup.backend.dto.AddToCartRequest
import com.lvlup.backend.dto.LoginRequest
import com.lvlup.backend.dto.RegisterRequest
import com.lvlup.backend.integration.BaseIntegrationTest
import com.lvlup.backend.repository.BooksRepository
import com.lvlup.backend.repository.CategoriesRepository
import com.lvlup.backend.repository.UserRepository
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@AutoConfigureMockMvc
class EndToEndUserJourneyTest(
    @Autowired var mockMvc: MockMvc
) : BaseIntegrationTest() {
    @Autowired private lateinit var userRepository: UserRepository
    private val logger = KotlinLogging.logger {}

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var bookRepository: BooksRepository

    @Autowired
    private lateinit var categoryRepository: CategoriesRepository

    private fun MockHttpServletRequestBuilder.withAuth(token: String): MockHttpServletRequestBuilder {
        return this.header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    @Test
    fun `complete user journey - register, browse, shop, and order`() {
        // Step 1: Register a new user
        val registerRequest = RegisterRequest(
            email = "shopper@example.com",
            password = "Shopper123!",
            firstName = "Happy",
            lastName = "Shopper"
        )

        val registerResult = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.token").exists())
            .andReturn()

        val registerResponse = objectMapper.readTree(registerResult.response.contentAsString)
        val userToken = registerResponse.get("data").get("token").asText()
        val userEmail = registerResponse.get("data").get("user").get("email").asText()

        logger.info(" Step 1: User registered with ID: $userEmail")

        // Step 2: Create some books to browse (as admin)
        val auth =
            UsernamePasswordAuthenticationToken("testuser", "password", listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        SecurityContextHolder.getContext().authentication = auth

        val category = categoryRepository.createCategory(TestFixtures.createCategoryFixture(name = "Programming"))
        val book1 = bookRepository.createBook(
            TestFixtures.createBookFixture(
                title = "Clean Code",
                author = "Robert Martin",
                price = BigDecimal("45.99"),
                stock = 100,
                categoryId = category.id!!
            )
        )
        val book2 = bookRepository.createBook(
            TestFixtures.createBookFixture(
                title = "Design Patterns",
                author = "Gang of Four",
                price = BigDecimal("54.99"),
                stock = 50,
                categoryId = category.id!!
            )
        )
        val book3 = bookRepository.createBook(
            TestFixtures.createBookFixture(
                title = "Refactoring",
                author = "Martin Fowler",
                price = BigDecimal("39.99"),
                stock = 75,
                categoryId = category.id!!
            )
        )

        logger.info(" Step 2: Created 3 books")

        // Step 3: Browse books (public endpoint)
        mockMvc.perform(
            get("/api/books")
                .param("page", "0")
                .param("size", "10")
                .withAuth(userToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.data").isArray)
            .andExpect(jsonPath("$.data.data.length()").value(3))
            .andExpect(jsonPath("$.data.totalElements").value(3))

        logger.info(" Step 3: Browsed available books")

        // Step 4: Search for specific books
        mockMvc.perform(
            get("/api/books")
                .param("title", "clean")
                .withAuth(userToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.data.length()").value(1))
            .andExpect(jsonPath("$.data.data[0].title").value("Clean Code"))

        logger.info(" Step 4: Searched for books by title")

        // Step 5: View cart (should be empty)
        mockMvc.perform(
            get("/api/shopping-cart")
                .withAuth(userToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(0))
            .andExpect(jsonPath("$.data.totalAmount").value(0))

        logger.info(" Step 5: Viewed empty cart")

        // Step 6: Add first book to cart
        mockMvc.perform(
            post("/api/shopping-cart/items/add")
                .withAuth(userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        AddToCartRequest(bookId = book1.id!!, quantity = 2)
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.totalItems").value(2))

        logger.info(" Step 6: Added 2 copies of 'Clean Code' to cart")

        // Step 7: Add second book to cart
        mockMvc.perform(
            post("/api/shopping-cart/items/add")
                .withAuth(userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        AddToCartRequest(bookId = book2.id!!, quantity = 1)
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.totalItems").value(3))

        logger.info(" Step 7: Added 1 copy of 'Design Patterns' to cart")

        // Step 8: Try to add same book again (should update quantity)
        mockMvc.perform(
            post("/api/shopping-cart/items/add")
                .withAuth(userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        AddToCartRequest(bookId = book1.id!!, quantity = 1)
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.totalItems").value(4))

        logger.info(" Step 8: Updated quantity of 'Clean Code' to 3")

        // Step 9: View updated cart
        val cartResult = mockMvc.perform(
            get("/api/shopping-cart")
                .withAuth(userToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.totalItems").value(4))
            .andReturn()

        val cartResponse = objectMapper.readTree(cartResult.response.contentAsString)
        val cartItemId = cartResponse.get("data").get("items").get(0).get("id").asLong()
        val totalAmount = cartResponse.get("data").get("totalAmount").asDouble()

        logger.info(" Step 9: Viewed cart - Total: $$totalAmount")

        // Step 10: Update quantity of an item
        mockMvc.perform(
            put("/api/shopping-cart/items/update/$cartItemId")
                .withAuth(userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\": 2}")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalItems").value(5))

        logger.info(" Step 10: Reduced quantity of first item to 2")

        // Step 11: View profile
        mockMvc.perform(
            get("/api/profile")
                .withAuth(userToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value(registerRequest.email))
            .andExpect(jsonPath("$.data.firstName").value(registerRequest.firstName))

        logger.info(" Step 11: Viewed profile")

        // Step 12: Check stock before order
        val book1BeforeOrder = bookRepository.findBookById(book1.id!!)!!
        val book2BeforeOrder = bookRepository.findBookById(book2.id!!)!!
        assertEquals(100, book1BeforeOrder.stock)
        assertEquals(50, book2BeforeOrder.stock)

        // TODO: Order flow

    }

    @Test
    fun `admin journey - manage inventory and orders`() {
        // Step 1: Login as admin
        val admin = userRepository.createUser(TestFixtures.createAdminFixture())
        val adminToken = authenticateAndGetToken(admin.email, "Admin123!")

        logger.info(" Admin logged in")

        // Step 2: Create category
        val categoryResult = mockMvc.perform(
            post("/api/categories")
                .withAuth(adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        TestFixtures.createCategoryRequest(name = "New Category")
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val categoryId = objectMapper.readTree(categoryResult.response.contentAsString)
            .get("id").asLong()

        logger.info(" Created category with ID: $categoryId")

        // Step 3: Create book
        val bookResult = mockMvc.perform(
            post("/api/books")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        TestFixtures.createBookRequest(
                            title = "Admin Book",
                            categoryId = categoryId
                        )
                    )
                )
        )
            .andExpect(status().isCreated)
            .andReturn()

        val bookId = objectMapper.readTree(bookResult.response.contentAsString)
            .get("id").asLong()

        logger.info(" Created book with ID: $bookId")

        // Step 4: Update book
        mockMvc.perform(
            put("/api/books/$bookId")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        TestFixtures.createBookRequest(
                            title = "Updated Book Title",
                            categoryId = categoryId,
                            stock = 200
                        )
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.title").value("Updated Book Title"))
            .andExpect(jsonPath("$.data.stock").value(200))

        logger.info(" Updated book")

        // Step 5: View all orders (admin privilege)
        mockMvc.perform(
            get("/api/orders")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.orders").isArray)

        logger.info(" Viewed all orders")

    }

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