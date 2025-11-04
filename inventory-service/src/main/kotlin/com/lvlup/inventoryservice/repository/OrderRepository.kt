package com.lvlup.inventoryservice.repository

import com.lvlup.inventoryservice.model.Book
import com.lvlup.inventoryservice.model.Category
import com.lvlup.inventoryservice.model.Order
import com.lvlup.inventoryservice.model.OrderItem
import com.lvlup.inventoryservice.model.OrderStatus
import com.lvlup.bookstore.jooq.tables.records.BooksRecord
import com.lvlup.bookstore.jooq.tables.records.CategoriesRecord
import com.lvlup.bookstore.jooq.tables.records.OrderItemsRecord
import com.lvlup.bookstore.jooq.tables.records.OrdersRecord
import com.lvlup.bookstore.jooq.tables.references.BOOKS
import com.lvlup.bookstore.jooq.tables.references.CATEGORIES
import com.lvlup.bookstore.jooq.tables.references.ORDERS
import com.lvlup.bookstore.jooq.tables.references.ORDER_ITEMS
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.jvm.java

@Repository
class OrderRepository(private val dsl: DSLContext) {

    fun findOrderById(id: Long): Order? {
        return dsl.selectFrom(ORDERS)
            .where(ORDERS.ID.eq(id))
            .fetchOne()?.mapToOrder()
    }

    fun findOrdersByUserEmail(userEmail: String, status: OrderStatus?, page: Int, size: Int): List<Order> {
        return dsl.selectFrom(ORDERS)
            .where(ORDERS.USER_EMAIL.eq(userEmail))
            .apply {
                if (status != null) {
                    this.and(ORDERS.STATUS.eq(status.name))
                }
            }
            .orderBy(ORDERS.CREATED_AT.desc())
            .limit(size)
            .offset(page * size)
            .fetch()
            .map { it.mapToOrder() }
    }

    fun countOrdersByUserEmail(userEmail: String): Long {
        return dsl.selectCount()
            .from(ORDERS)
            .where(ORDERS.USER_EMAIL.eq(userEmail))
            .fetchOne(0, Long::class.java) ?: 0L
    }

    fun findAllOrders(status: OrderStatus?, page: Int, size: Int): List<Order> {
        return dsl.selectFrom(ORDERS)
            .apply {
                if (status != null) {
                    this.where(ORDERS.STATUS.eq(status.name))
                }
            }.orderBy(ORDERS.CREATED_AT.desc())
            .limit(size)
            .offset(page * size)
            .fetch()
            .map { it.mapToOrder() }
    }

    fun countAllOrders(): Long {
        return dsl.selectCount()
            .from(ORDERS)
            .fetchOne(0, Long::class.java) ?: 0L
    }

    @Transactional
    fun createNewOrder(order: Order): Order {
        return dsl.newRecord(ORDERS).apply {
            userEmail = order.userEmail
            orderNumber = order.orderNumber
            totalAmount = order.totalAmount
            status = order.status.name
            shippingAddress = order.shippingAddress
            createdAt = order.createdAt
            updatedAt = order.updatedAt
        }.let { newOrderRecord ->
            newOrderRecord.store()
            newOrderRecord.mapToOrder()
        }
    }

    @Transactional
    fun updateStatus(orderId: Long, status: OrderStatus): Order? {
        return dsl.update(ORDERS)
            .set(ORDERS.STATUS, status.name)
            .set(ORDERS.UPDATED_AT, LocalDateTime.now())
            .where(ORDERS.ID.eq(orderId))
            .returning().fetchOne()?.mapToOrder()
    }

    fun findOrderItemsAndTheirBooks(orderId: Long): List<Pair<OrderItem, Book>> {
        return dsl.select(
            ORDER_ITEMS.asterisk(),
            BOOKS.asterisk(),
            CATEGORIES.asterisk()
        )
            .from(ORDER_ITEMS)
            .leftJoin(BOOKS)
            .on(ORDER_ITEMS.BOOK_ID.eq(BOOKS.ID))
            .leftJoin(CATEGORIES)
            .on(BOOKS.CATEGORY_ID.eq(CATEGORIES.ID))
            .where(ORDER_ITEMS.ORDER_ID.eq(orderId))
            .fetch()
            .map { mapOrderItemToBookPair(it) }
    }

    @Transactional
    fun saveOrderItem(orderItem: OrderItem): OrderItem {
        return dsl.newRecord(ORDER_ITEMS).apply {
            this.orderId = orderItem.orderId
            this.bookId = orderItem.bookId
            this.quantity = orderItem.quantity
            this.priceAtPurchase = orderItem.priceAtPurchase
            this.createdAt = orderItem.createdAt
        }.let { newOrderItem ->
            newOrderItem.store()
            newOrderItem.mapToOrderItem()
        }
    }

    private fun OrdersRecord.mapToOrder(): Order {
        return Order(
            id = id,
            userEmail = userEmail!!,
            orderNumber = orderNumber!!,
            totalAmount = totalAmount!!,
            status = OrderStatus.valueOf(status!!),
            shippingAddress = shippingAddress!!,
            createdAt = createdAt!!,
            updatedAt = updatedAt!!
        )
    }

    private fun OrderItemsRecord.mapToOrderItem(): OrderItem {
        return OrderItem(
            id = id,
            orderId = orderId!!,
            bookId = bookId!!,
            quantity = quantity!!,
            priceAtPurchase = priceAtPurchase!!,
            createdAt = createdAt!!
        )
    }

    private fun mapOrderItemToBookPair(resultRecord: Record): Pair<OrderItem, Book> {
        val extractedOrderItem =
            resultRecord.into(OrderItemsRecord::class.java).mapToOrderItem()
        val extractedBookRecord = resultRecord.into(BooksRecord::class.java)
        val extractedCategoryRecord = resultRecord.into(CategoriesRecord::class.java)

        return extractedOrderItem to Book(
            id = extractedBookRecord.id,
            title = extractedBookRecord.title!!,
            author = extractedBookRecord.author!!,
            description = extractedBookRecord.description,
            price = extractedBookRecord.price!!,
            stock = extractedBookRecord.stock!!,
            category = Category(
                id = extractedCategoryRecord.id,
                name = extractedCategoryRecord.name!!,
                description = extractedCategoryRecord.description,
                createdAt = extractedCategoryRecord.createdAt!!,
                updatedAt = extractedCategoryRecord.updatedAt!!,
            ),
            coverImageUrl = extractedBookRecord.coverImageUrl,
            createdAt = extractedBookRecord.createdAt!!,
            updatedAt = extractedBookRecord.updatedAt!!,
            deleted = extractedBookRecord.deleted!!,
        )
    }

}