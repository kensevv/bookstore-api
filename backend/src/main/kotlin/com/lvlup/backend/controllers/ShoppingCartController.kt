package com.lvlup.backend.controllers

import com.lvlup.backend.dto.AddToCartRequest
import com.lvlup.backend.dto.ApiResponseFactory
import com.lvlup.backend.dto.CartResponse
import com.lvlup.backend.dto.SuccessResponse
import com.lvlup.backend.dto.UpdateCartItemRequest
import com.lvlup.backend.service.ShoppingCartService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/shopping-cart")
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
