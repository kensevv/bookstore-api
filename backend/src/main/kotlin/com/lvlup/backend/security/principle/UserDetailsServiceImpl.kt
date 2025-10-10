package com.lvlup.backend.security.principle

import com.lvlup.backend.repository.UserRepository
import mu.KotlinLogging
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {
    private val logger = KotlinLogging.logger {}

    override fun loadUserByUsername(username: String): UserDetailsImpl {
        logger.debug { "Loading user by username: $username" }
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User not found with username: $username")
        return UserDetailsImpl(user)
    }
}