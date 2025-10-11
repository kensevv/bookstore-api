package com.lvlup.backend.service

import com.lvlup.backend.dto.ChangePasswordRequest
import com.lvlup.backend.dto.UpdateProfileRequest
import com.lvlup.backend.dto.UserResponse
import com.lvlup.backend.exception.InvalidCredentialsException
import com.lvlup.backend.exception.UserNotFoundException
import com.lvlup.backend.repository.UserRepository
import com.lvlup.backend.security.principle.UserDetailsImpl
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationService: AuthenticationService
) {
    private val logger = KotlinLogging.logger {}


    fun getCurrentUserDto(): UserResponse {
        val authentication = SecurityContextHolder.getContext().authentication
        val userPrincipal = authentication.principal as UserDetailsImpl
        return UserResponse(userPrincipal)
    }

    fun changePassword(changePasswordRequest: ChangePasswordRequest) {
        val currentUser = authenticationService.getCurrentUser()
        logger.info("Password change request for user: ${currentUser.email}")

        if (!passwordEncoder.matches(changePasswordRequest.currentPassword, currentUser.passwordHash)) {
            logger.warn("Password change failed: Invalid current password for user: ${currentUser.email}")
            throw InvalidCredentialsException("Current password is incorrect")
        }

        if (changePasswordRequest.currentPassword == changePasswordRequest.newPassword) {
            throw InvalidCredentialsException("New password must be different from current password")
        }

        userRepository.updatePassword(
            currentUser.email,
            passwordEncoder.encode(changePasswordRequest.newPassword)
        )

        logger.info("Password changed successfully for user: ${currentUser.email}")
    }

    fun updateProfile(profileUpdateRequest: UpdateProfileRequest): UserResponse {
        val currentUser = authenticationService.getCurrentUser()

        logger.info("Profile update request for user: ${currentUser.email}")

        val updatedUser = userRepository.updateUserProfile(
            currentUser.email,
            profileUpdateRequest.firstName, profileUpdateRequest.lastName
        ) ?: throw UserNotFoundException("User not found with email: ${currentUser.email}")

        logger.info("Profile updated successfully for user: ${currentUser.email}")

        return UserResponse(updatedUser)
    }
}