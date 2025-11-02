package com.lvlup.getaway.auth

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import kotlin.text.startsWith
import kotlin.text.substring

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val path = exchange.request.uri.path

        if (path.startsWith("/api/auth/")) {
            return chain.filter(exchange)
        }

        val token = getTokenFromExchange(exchange)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val userId = jwtTokenProvider.getEmailFromToken(token)
            val role = jwtTokenProvider.getRolesFromToken(token)

            val modifiedRequest = exchange.request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", role)
                .build()

            return chain.filter(exchange.mutate().request(modifiedRequest).build())
        } else {
            exchange.response.statusCode = HttpStatus.UNAUTHORIZED
            return exchange.response.setComplete()
        }
    }

    private fun getTokenFromExchange(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null
        }

        return authHeader.substring(7)
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}