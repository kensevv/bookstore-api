package com.lvlup.inventoryservice.repository

import com.lvlup.inventoryservice.model.Order
import com.lvlup.inventoryservice.model.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OrderRepository : JpaRepository<Order, Long> {

    fun findByIdAndUserEmail(id: Long, userEmail: String): Optional<Order>

    @Query(
        """
        SELECT o FROM Order o
        WHERE o.userEmail = :userEmail
        AND (:status IS NULL OR o.status = :status)
    """
    )
    fun findAllByUserEmailAndStatus(userEmail: String, status: OrderStatus?, pageable: Pageable): Page<Order>

    @Query(
        """
        SELECT o FROM Order o
        WHERE :status IS NULL OR o.status = :status
    """
    )
    fun findAllByStatus(status: OrderStatus?, pageable: Pageable): Page<Order>
}