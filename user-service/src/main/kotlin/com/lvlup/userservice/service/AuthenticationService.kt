package com.lvlup.userservice.service

import com.lvlup.commonspring.exception.InvalidTokenException
import com.lvlup.userservice.dto.AuthJwtResponse
import com.lvlup.userservice.dto.LoginRequest
import com.lvlup.userservice.dto.RegisterRequest
import com.lvlup.userservice.dto.UserResponse
import com.lvlup.userservice.exception.InvalidCredentialsException
import com.lvlup.userservice.exception.UserAlreadyExistsException
import com.lvlup.userservice.exception.UserNotFoundException
import com.lvlup.userservice.model.User
import com.lvlup.userservice.model.UserRole
import com.lvlup.userservice.repository.UserRepository
import com.lvlup.userservice.security.jwt.JwtTokenProvider
import com.lvlup.userservice.security.principle.UserDetailsImpl
import mu.KotlinLogging
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager
) {
    private val logger = KotlinLogging.logger {}

    @Transactional(readOnly = true)
    fun fetchCurrentlySignedInUser(): User {
        val authentication = SecurityContextHolder.getContext().authentication
        val userEmail = authentication.principal as String

        return userRepository.findByEmail(userEmail)
            ?: throw UserNotFoundException("User not found with email: $userEmail")
    }

    @Transactional
    fun login(request: LoginRequest): AuthJwtResponse {
        logger.debug { "Login attempt for user: ${request.email}" }

        val authentication = runCatching {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )
        }.onFailure { ex ->
            when (ex) {
                is AuthenticationException -> {
                    logger.debug { "Invalid credentials for ${request.email}" }
                    throw InvalidCredentialsException("Invalid email or password")
                }

                else -> logger.error(ex) { "Unexpected error during login for ${request.email}" }
            }
        }.getOrThrow()

        SecurityContextHolder.getContext().authentication = authentication

        val userPrincipal = authentication.principal as UserDetailsImpl
        val token = jwtTokenProvider.generateToken(userPrincipal)
        val refreshToken = jwtTokenProvider.generateToken(userPrincipal, isRefreshToken = true)

        logger.debug { "User logged in successfully: ${request.email}" }

        return AuthJwtResponse(
            token = token,
            refreshToken = refreshToken,
            user = UserResponse(userPrincipal)
        )
    }

    @Transactional
    fun registerNewUser(request: RegisterRequest): AuthJwtResponse {
        logger.debug { "Attempting to register user with email: ${request.email}" }

        if (userRepository.existsByEmail(request.email)) {
            logger.debug { "Registration failed: Email already exists - ${request.email}" }
            throw UserAlreadyExistsException("User with email ${request.email} already exists")
        }


        val createdTimestamp = LocalDateTime.now()
        val newUser = User(
            email = request.email,
            firstName = request.firstName,
            lastName = request.lastName,
            passwordHash = passwordEncoder.encode(request.password),
            role = UserRole.ROLE_USER,
            createdAt = createdTimestamp,
            updatedAt = createdTimestamp,
        )

        val createdUser = userRepository.save(newUser)
        logger.info("User: ${createdUser.email} registered successfully.")

        val userPrincipal = UserDetailsImpl(createdUser)
        val token = jwtTokenProvider.generateToken(userPrincipal)
        val refreshToken = jwtTokenProvider.generateToken(userPrincipal, isRefreshToken = true)

        return AuthJwtResponse(
            token = token,
            refreshToken = refreshToken,
            user = UserResponse(userPrincipal)
        )
    }

    @Transactional
    fun refreshToken(refreshToken: String): AuthJwtResponse {
        logger.debug { "Refresh token attempt" }

        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            logger.debug { "Invalid refresh token" }
            throw InvalidTokenException("Invalid refresh token")
        }

        val userEmail = jwtTokenProvider.getEmailFromToken(refreshToken)

        val user = userRepository.findByEmail(userEmail)
            ?: throw UserNotFoundException("User not found with email: $userEmail")

        val userDetails = UserDetailsImpl(user)

        val newToken = jwtTokenProvider.generateToken(userDetails)
        val newRefreshToken = jwtTokenProvider.generateToken(userDetails, isRefreshToken = true)

        logger.info { "Token refreshed successfully for user: $userEmail" }

        return AuthJwtResponse(
            token = newToken,
            refreshToken = newRefreshToken,
            user = UserResponse(userDetails)
        )
    }

}