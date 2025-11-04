package com.lvlup.inventoryservice.controllers

import com.lvlup.inventoryservice.dto.AddToCartRequest
import com.lvlup.inventoryservice.dto.CartResponse
import com.lvlup.inventoryservice.dto.UpdateCartItemRequest
import com.lvlup.inventoryservice.service.ShoppingCartService
import dto.ApiResponseFactory
import dto.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/inventory/shopping-cart")
class ShoppingCartController(
    private val cartService: ShoppingCartService
) {
    @GetMapping
    fun getUserShoppingCart(principal: Principal): ResponseEntity<SuccessResponse<CartResponse>> {
        val response = cartService.getUserCartAndRemoveInvalidItems(principal.name)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @PostMapping("/items/add")
    fun addItemToCart(
        @Valid @RequestBody request: AddToCartRequest,
        principal: Principal,
    ): ResponseEntity<SuccessResponse<CartResponse>> {
        val response = cartService.addItemToUserCart(principal.name, request)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @PutMapping("/items/update/{itemId}")
    fun updateCartItemQuantity(
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateCartItemRequest,
        principal: Principal,
    ): ResponseEntity<SuccessResponse<CartResponse>> {
        val response = cartService.updateCartItemQuantity(principal.name, itemId, request)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @DeleteMapping("/items/delete/{itemId}")
    fun removeItemFromCart(
        @PathVariable itemId: Long,
        principal: Principal,
    ): ResponseEntity<SuccessResponse<CartResponse>> {
        val response = cartService.removeItemFromUserCart(principal.name, itemId)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }
}
