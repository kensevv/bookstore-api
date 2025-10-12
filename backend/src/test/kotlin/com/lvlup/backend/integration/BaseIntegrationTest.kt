package com.lvlup.backend.integration

import com.lvlup.bookstore.jooq.tables.Books
import com.lvlup.bookstore.jooq.tables.ShoppingCarts.Companion.SHOPPING_CARTS
import com.lvlup.bookstore.jooq.tables.references.BOOKS
import com.lvlup.bookstore.jooq.tables.references.CATEGORIES
import com.lvlup.bookstore.jooq.tables.references.SHOPPING_CARTS_ITEMS
import com.lvlup.bookstore.jooq.tables.references.USERS
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
abstract class BaseIntegrationTest {

    @Autowired
    protected lateinit var dsl: DSLContext

    @BeforeEach
    fun cleanDatabase() {
        dsl.deleteFrom(SHOPPING_CARTS_ITEMS).execute()
        dsl.deleteFrom(SHOPPING_CARTS).execute()
        dsl.deleteFrom(BOOKS).execute()
        dsl.deleteFrom(CATEGORIES).execute()
        dsl.deleteFrom(USERS).execute()
    }

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("bookstore")
            .withUsername("bookstore")
            .withPassword("bookstore123")
            .withInitScript("init-test.sql")

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.liquibase.default-schema") { "bookstore" }
        }
    }
}