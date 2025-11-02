package com.lvlup.userservice.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "Current password is required")
    val currentPassword: String,

    @field:NotBlank(message = "New password is required")
    @field:Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    val newPassword: String
)

data class UpdateProfileRequest(
    @field:NotBlank(message = "First name is required")
    @field:Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    val firstName: String,

    @field:NotBlank(message = "Last name is required")
    @field:Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    val lastName: String
)