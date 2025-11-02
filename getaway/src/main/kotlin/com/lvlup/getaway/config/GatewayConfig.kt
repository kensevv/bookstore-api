package com.lvlup.getaway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig {

    @Bean
    fun routerBuilder(routeLocatorBuilder: RouteLocatorBuilder): RouteLocator {
        return routeLocatorBuilder.routes()
            .route("user-service", { r ->
                r.path("/api/auth/**", "/api/profile/**")
                    .uri("http://localhost:8081/")
            })
            .build()
    }
}