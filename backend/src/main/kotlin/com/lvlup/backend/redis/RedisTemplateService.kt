package com.lvlup.backend.redis

import mu.KotlinLogging
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service


@Service
class RedisTemplateService(private val redisTemplate: RedisTemplate<String, Any>) {

    private val logger = KotlinLogging.logger {}

    fun saveValue(cacheName: String, key: String, value: Any) {
        runCatching {
            redisTemplate.opsForValue().set(
                "$cacheName:$key", value
            )
        }.onFailure {
            logger.debug { "FAILED: Cache PUT in '${cacheName}' for key '$key'" }
        }.onSuccess {
            logger.debug { "Cache PUT in '${cacheName}' for key '$key'" }
        }
    }
}