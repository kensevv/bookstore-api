package com.lvlup.getaway

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean


@SpringBootApplication
class GatewayApplication

fun main(args: Array<String>) {
    SpringApplication.run(GatewayApplication::class.java, *args)
}