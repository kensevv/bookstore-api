package com.lvlup.backend.service

import com.lvlup.backend.model.User
import com.lvlup.backend.exception.InvalidCredentialsException
import com.lvlup.backend.exception.UserAlreadyExistsException
import com.lvlup.backend.dto.AuthJwtResponse
import com.lvlup.backend.dto.LoginRequest
import com.lvlup.backend.dto.RegisterRequest
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
import java.time.LocalDateTime

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager
) {
    private val logger = KotlinLogging.logger {}

    fun login(request: LoginRequest): AuthJwtResponse {
        logger.debug { "Login attempt for user: ${request.username}" }

        val authentication = runCatching {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, request.password)
            )
        }.onFailure { ex ->
            when (ex) {
                is AuthenticationException -> {
                    logger.debug { "Invalid credentials for ${request.username}" }
                    throw InvalidCredentialsException("Invalid username or password")
                }

                else -> logger.error(ex) { "Unexpected error during login for ${request.username}" }
            }
        }.getOrThrow()

        SecurityContextHolder.getContext().authentication = authentication

        val userPrincipal = authentication.principal as UserDetailsImpl
        val token = jwtTokenProvider.generateToken(userPrincipal)
        val refreshToken = jwtTokenProvider.generateToken(userPrincipal, isRefreshToken = true)

        logger.debug { "User logged in successfully: ${request.username}" }

        return AuthJwtResponse(
            token = token,
            refreshToken = refreshToken,
            username = userPrincipal.username,
            role = userPrincipal.getUserRole(),
        )
    }

    fun registerNewUser(request: RegisterRequest): AuthJwtResponse {
        logger.debug { "Attempting to register user with username: ${request.username}" }

        require(!userRepository.existsByUsername(request.username)) {
            logger.debug { "Registration failed: Email already exists - ${request.username}" }
            throw UserAlreadyExistsException("User with username ${request.username} already exists")
        }

        val newUser = User(
            username = request.username,
            passwordHash = passwordEncoder.encode(request.password),
            role = UserRole.ROLE_USER,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        userRepository.createUser(newUser)
        logger.info("User: ${newUser.username} registered successfully.")

        val userPrincipal = UserDetailsImpl(newUser)
        val token = jwtTokenProvider.generateToken(userPrincipal)
        val refreshToken = jwtTokenProvider.generateToken(userPrincipal, isRefreshToken = true)

        return AuthJwtResponse(
            token = token,
            refreshToken = refreshToken,
            username = newUser.username,
            role = newUser.role,
        )
    }

}

