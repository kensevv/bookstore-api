package com.lvlup.userservice.service

import com.lvlup.userservice.dto.ChangePasswordRequest
import com.lvlup.userservice.dto.UpdateProfileRequest
import com.lvlup.userservice.dto.UserResponse
import com.lvlup.userservice.exception.InvalidCredentialsException
import com.lvlup.userservice.exception.UserNotFoundException
import com.lvlup.userservice.model.User
import com.lvlup.userservice.repository.UserRepository
import mu.KotlinLogging
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserProfileService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationService: AuthenticationService
) {
    private val logger = KotlinLogging.logger {}

    fun getCurrentUserDto() = UserResponse(authenticationService.fetchCurrentlySignedInUser())

    @Transactional
    fun changePassword(userEmail: String, changePasswordRequest: ChangePasswordRequest) {
        val currentUser: User = userRepository.findByEmail(userEmail) ?: throw UserNotFoundException()
        logger.info("Password change request for user: ${currentUser.email}")

        if (!passwordEncoder.matches(changePasswordRequest.currentPassword, currentUser.passwordHash)) {
            logger.warn("Password change failed: Invalid current password for user: ${currentUser.email}")
            throw InvalidCredentialsException("Current password is incorrect")
        }

        if (changePasswordRequest.currentPassword == changePasswordRequest.newPassword) {
            throw InvalidCredentialsException("New password must be different from current password")
        }

        currentUser.apply {
            passwordHash = passwordEncoder.encode(changePasswordRequest.newPassword)
            updatedAt = LocalDateTime.now()
        }

        userRepository.save(currentUser)

        logger.info("Password changed successfully for user: ${currentUser.email}")
    }

    @Transactional
    fun updateProfile(profileUpdateRequest: UpdateProfileRequest): UserResponse {
        val currentUser = authenticationService.fetchCurrentlySignedInUser()

        logger.info("Profile update request for user: ${currentUser.email}")

        currentUser.apply {
            firstName = profileUpdateRequest.firstName
            lastName = profileUpdateRequest.lastName
            updatedAt = LocalDateTime.now()
        }
        val updatedUser = userRepository.save(currentUser)
        logger.info("Profile updated successfully for user: ${currentUser.email}")

        return UserResponse(updatedUser)
    }
}