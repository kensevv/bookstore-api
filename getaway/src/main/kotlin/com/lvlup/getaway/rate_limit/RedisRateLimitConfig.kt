package com.lvlup.getaway.rate_limit

import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisRateLimitConfig {
    @Bean
    fun redisRateLimiter(): RedisRateLimiter {
        return RedisRateLimiter(1, 10)
    }
}