package com.lvlup.backend.redis


import mu.KotlinLogging
import org.springframework.cache.Cache
import java.util.concurrent.Callable

private val logger = KotlinLogging.logger {}

class LoggingCache(private val delegate: Cache) : Cache {

    override fun getName(): String = delegate.name
    override fun getNativeCache(): Any = delegate.nativeCache

    override fun get(key: Any): Cache.ValueWrapper? {
        val result = delegate.get(key)
        if (result != null) {
            logger.debug { "Cache HIT in '${delegate.name}' for key '$key'" }
        } else {
            logger.debug { "Cache MISS in '${delegate.name}' for key '$key'" }
        }
        return result
    }

    override fun <T : Any?> get(key: Any, type: Class<T?>?): T? {
        val result = delegate.get(key, type)
        if (result != null) {
            logger.debug { "Cache HIT in '${delegate.name}' for key '$key'" }
        } else {
            logger.debug { "Cache MISS in '${delegate.name}' for key '$key'" }
        }
        return result
    }

    override fun <T : Any?> get(key: Any, valueLoader: Callable<T?>): T? {
        val value = delegate.get(key, valueLoader)
        logger.debug { "Cache PUT in '${delegate.name}' for key '$key'" }
        return value
    }

    override fun put(key: Any, value: Any?) {
        delegate.put(key, value)
        logger.debug { "Cache PUT in '${delegate.name}' for key '$key'" }
    }

    override fun evict(key: Any) {
        delegate.evict(key)
        logger.debug { "Cache EVICT in '${delegate.name}' for key '$key'" }
    }

    override fun clear() {
        delegate.clear()
        logger.debug { "Cache CLEARED for '${delegate.name}'" }
    }
}
