package com.lvlup.userservice.security.principle

import com.lvlup.userservice.repository.UserRepository
import mu.KotlinLogging
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {
    private val logger = KotlinLogging.logger {}

    override fun loadUserByUsername(email: String): UserDetailsImpl {
        logger.debug { "Loading user by email: $email" }
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")
        return UserDetailsImpl(user)
    }
}