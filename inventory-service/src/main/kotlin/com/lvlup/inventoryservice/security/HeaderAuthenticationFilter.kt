package com.lvlup.inventoryservice.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class HeaderAuthenticationFilter() : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val userId = request.getHeader("X-User-Id")
        val role = request.getHeader("X-User-Role")
//        val userId = "kenan"
//        val role = "ROLE_ADMIN"

        if (userId != null && role != null) {
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                userId,
                null,
                listOf(SimpleGrantedAuthority(role))
            )
        }

        filterChain.doFilter(request, response)
    }
}