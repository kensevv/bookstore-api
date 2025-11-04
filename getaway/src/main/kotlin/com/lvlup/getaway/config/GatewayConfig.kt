package com.lvlup.getaway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.BooleanSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.UriSpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig(
    private val redisRateLimiter: RedisRateLimiter,
    private val userOrIpKeyResolver: KeyResolver
) {

    @Bean
    fun routerBuilder(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator {
        return routeLocatorBuilder.routes()
            .addRoute("user-service", 8081, "/api/auth/**", "/api/profile/**")
            .addRoute("inventory-service", 8082, "/api/inventory/**")
            .build()
    }

    private fun RouteLocatorBuilder.Builder.addRoute(
        serviceName: String,
        port: Int,
        vararg pattern: String
    ): RouteLocatorBuilder.Builder {
        return this.route(serviceName) { r ->
            r.path(*pattern)
                .addCustomRateLimitFilter()
                .uri("http://localhost:${port}/")
        }

    }

    private fun BooleanSpec.addCustomRateLimitFilter(): UriSpec {
        return this.filters { gatewayFilterSpec ->
            gatewayFilterSpec.requestRateLimiter { config ->
                config.rateLimiter = redisRateLimiter
                config.keyResolver = userOrIpKeyResolver
            }
        }
    }
}