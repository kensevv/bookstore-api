package com.lvlup.backend.service

import com.lvlup.backend.model.User
import com.lvlup.backend.exception.InvalidCredentialsException
import com.lvlup.backend.exception.UserAlreadyExistsException
import com.lvlup.backend.dto.AuthJwtResponse
import com.lvlup.backend.dto.LoginRequest
import com.lvlup.backend.dto.RegisterRequest
import com.lvlup.backend.dto.UserResponse
import com.lvlup.backend.exception.UserNotFoundException
import com.lvlup.backend.model.UserRole
import com.lvlup.backend.repository.UserRepository
import com.lvlup.backend.security.jwt.JwtTokenProvider
import com.lvlup.backend.security.principle.UserDetailsImpl
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
        val userPrincipal = authentication.principal as UserDetailsImpl

        return userRepository.findUserByEmail(userPrincipal.username)
            ?: throw UserNotFoundException("User not found with email: ${userPrincipal.username}")
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

        if (userRepository.existsUserByEmail(request.email)) {
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

        userRepository.createUser(newUser)
        logger.info("User: ${newUser.email} registered successfully.")

        val userPrincipal = UserDetailsImpl(newUser)
        val token = jwtTokenProvider.generateToken(userPrincipal)
        val refreshToken = jwtTokenProvider.generateToken(userPrincipal, isRefreshToken = true)

        return AuthJwtResponse(
            token = token,
            refreshToken = refreshToken,
            user = UserResponse(userPrincipal)
        )
    }

}

