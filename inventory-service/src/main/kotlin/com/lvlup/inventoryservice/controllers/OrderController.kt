package com.lvlup.inventoryservice.controllers

import com.lvlup.inventoryservice.dto.CreateOrderRequest
import com.lvlup.inventoryservice.dto.OrderResponse
import com.lvlup.inventoryservice.model.OrderStatus
import com.lvlup.inventoryservice.service.OrderService
import dto.ApiResponseFactory
import dto.PaginatedDataResponse
import dto.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/inventory/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping("/create")
    fun createOrder(
        @Valid @RequestBody request: CreateOrderRequest,
        principal: Principal
    ): ResponseEntity<SuccessResponse<OrderResponse>> {
        val response = orderService.createUserOrder(principal.name, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseFactory.success(response))
    }

    @GetMapping("/my-orders")
    fun getCurrentUserOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: OrderStatus?,
        principal: Principal,
    ): ResponseEntity<SuccessResponse<PaginatedDataResponse<OrderResponse>>> {
        val response =
            orderService.getOrdersPaginatedByUserEmail(principal.name, page, size, status)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @GetMapping("/my-orders/{orderId}")
    fun getCurrentUserOrderById(
        @PathVariable orderId: Long,
        principal: Principal,
    ): ResponseEntity<SuccessResponse<OrderResponse>> {
        val response = orderService.getUserOrderById(orderId, principal.name)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllPaginatedOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: OrderStatus?
    ): ResponseEntity<SuccessResponse<PaginatedDataResponse<OrderResponse>>> {
        val response = orderService.getAllOrdersPaginated(page, size, status)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @PutMapping("/{orderId}/change-status")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateOrderStatus(
        @PathVariable orderId: Long,
        @RequestParam newStatus: OrderStatus,
    ): ResponseEntity<SuccessResponse<OrderResponse>> {
        val response = orderService.updateOrderStatus(orderId, newStatus)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }
}