package com.lvlup.getaway.auth

import exception.InvalidTokenException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date


@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
) {

    private val logger = KotlinLogging.logger {}

    private val key = lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun validateToken(token: String): Boolean = runCatching {
        !isTokenExpired(token) && getTokenTypeFromToken(token) == "token"
    }.onFailure { exception ->
        when (exception) {
            is SecurityException -> logger.error("Invalid JWT signature", exception)
            is MalformedJwtException -> logger.error("Invalid JWT token", exception)
            is ExpiredJwtException -> logger.error("Expired JWT token", exception)
            is UnsupportedJwtException -> logger.error("Unsupported JWT token", exception)
            is IllegalArgumentException -> logger.error("JWT claims string is empty", exception)
        }
    }.getOrDefault(false)


    fun getEmailFromToken(token: String): String = runCatching {
        getAllClaimsFromToken(token).subject
    }.getOrElse { ex ->
        logger.error("Failed to extract email from token", ex)
        throw InvalidTokenException("Invalid token")
    }

    fun getRolesFromToken(token: String): String = runCatching {
        getAllClaimsFromToken(token).get("role", String::class.java)
    }.getOrElse { ex ->
        logger.error("Failed to extract email from token", ex)
        throw InvalidTokenException("Invalid token")
    }


    private fun getAllClaimsFromToken(token: String?): Claims =
        Jwts.parserBuilder().setSigningKey(key.value).build().parseClaimsJws(token).body

    private fun getTokenTypeFromToken(token: String): String {
        return getAllClaimsFromToken(token).get("tokenType", String::class.java)
    }

    private fun getExpirationDateFromToken(token: String): Date = runCatching {
        getAllClaimsFromToken(token).expiration
    }.getOrElse { ex ->
        logger.error("Failed to extract expiration from token", ex)
        throw InvalidTokenException("Invalid token")
    }

    private fun isTokenExpired(token: String): Boolean {
        return try {
            getExpirationDateFromToken(token).before(Date())
        } catch (_: Exception) {
            true
        }
    }

}
