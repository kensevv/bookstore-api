package com.lvlup.userservice.controller

import com.lvlup.userservice.dto.AuthJwtResponse
import com.lvlup.userservice.dto.LoginRequest
import com.lvlup.userservice.dto.RegisterRequest
import com.lvlup.userservice.service.AuthenticationService
import dto.ApiResponseFactory
import dto.SuccessResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/auth")
class AuthenticationController(
    private val authService: AuthenticationService
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<SuccessResponse<AuthJwtResponse>> {
        val response = authService.registerNewUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseFactory.success(data = response))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<SuccessResponse<AuthJwtResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponseFactory.success(data = response))
    }

    @PostMapping("/refresh-token")
    fun refreshToken(@RequestParam refreshToken: String): ResponseEntity<SuccessResponse<AuthJwtResponse>> {
        val response = authService.refreshToken(refreshToken)
        return ResponseEntity.ok(ApiResponseFactory.success(data = response))
    }

}