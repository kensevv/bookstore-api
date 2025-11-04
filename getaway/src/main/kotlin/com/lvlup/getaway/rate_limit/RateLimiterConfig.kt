package com.lvlup.getaway.rate_limit

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono

@Configuration
class RateLimiterConfig {

    /**
     * Use user ID (from JWT filter) or fallback to IP.
     */
    @Bean
    fun userOrIpKeyResolver(): KeyResolver = KeyResolver { exchange ->
        val userId = exchange.request.headers.getFirst("X-User-Id")
        if (!userId.isNullOrBlank()) {
            Mono.just("user:$userId")
        } else {
            val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
            Mono.just("ip:$ip")
        }
    }
}

