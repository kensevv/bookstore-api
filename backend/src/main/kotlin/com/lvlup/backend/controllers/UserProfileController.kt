package com.lvlup.backend.controllers

import com.lvlup.backend.dto.ApiResponseFactory
import com.lvlup.backend.dto.ChangePasswordRequest
import com.lvlup.backend.dto.SuccessResponse
import com.lvlup.backend.dto.UpdateProfileRequest
import com.lvlup.backend.dto.UserResponse
import com.lvlup.backend.service.UserProfileService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<SuccessResponse<Unit>> {
        userProfileService.changePassword(request)
        return ResponseEntity.ok(
            ApiResponseFactory.success(message = "Password changed successfully")
        )
    }

}