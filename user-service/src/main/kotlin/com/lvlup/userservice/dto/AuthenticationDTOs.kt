package com.lvlup.userservice.dto

import com.lvlup.userservice.model.User
import com.lvlup.userservice.model.UserRole
import com.lvlup.userservice.security.principle.UserDetailsImpl
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String
)

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email must be valid")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    val password: String,

    @field:NotBlank(message = "First name is required")
    @field:Size(min = 3, max = 100, message = "First name must be between 1 and 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    val lastName: String
)

data class AuthJwtResponse(
    val token: String,
    val refreshToken: String,
    val type: String = "Bearer",
    val user: UserResponse
)

data class UserResponse(
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole
) {
    constructor(userDetails: UserDetailsImpl) : this(
        userDetails.username,
        userDetails.getFirstName(),
        userDetails.getLastName(),
        userDetails.getUserRole()
    )

    constructor(user: User) : this(
        user.email,
        user.firstName,
        user.lastName,
        user.role
    )

}

