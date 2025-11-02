package com.lvlup.userservice.controller

import com.lvlup.userservice.service.UserProfileService

import com.lvlup.userservice.dto.ChangePasswordRequest
import com.lvlup.userservice.dto.UpdateProfileRequest
import com.lvlup.userservice.dto.UserResponse
import dto.ApiResponseFactory
import dto.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/profile")
class UserProfileController(
    private val userProfileService: UserProfileService
) {
    @GetMapping
    fun getCurrentUser(): ResponseEntity<SuccessResponse<UserResponse>> {
        val currentUser = userProfileService.getCurrentUserDto()
        return ResponseEntity.ok(ApiResponseFactory.success(currentUser))
    }

    @PutMapping("/update-profile")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<SuccessResponse<UserResponse>> {
        val response = userProfileService.updateProfile(request)
        return ResponseEntity.ok(ApiResponseFactory.success(response))
    }

    @PutMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        principal: Principal
    ): ResponseEntity<SuccessResponse<Unit>> {
        userProfileService.changePassword(principal.name, request)
        return ResponseEntity.ok(
            ApiResponseFactory.success(message = "Password changed successfully")
        )
    }

}