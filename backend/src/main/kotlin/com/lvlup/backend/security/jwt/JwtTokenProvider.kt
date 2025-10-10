package com.lvlup.backend.security.jwt

import com.lvlup.backend.exception.InvalidTokenException
import com.lvlup.backend.security.principle.UserDetailsImpl
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date


@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.token.expiration}") private val jwtExpirationMs: String,
    @Value("\${jwt.refreshtoken.expiration}") private val jwtRefreshExpirationMs: String,
) {

    private val logger = KotlinLogging.logger {}

    private val key = lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun validateToken(token: String): Boolean = runCatching {
        !isTokenExpired(token)
    }.onFailure { exception ->
        when (exception) {
            is SecurityException -> logger.error("Invalid JWT signature", exception)
            is MalformedJwtException -> logger.error("Invalid JWT token", exception)
            is ExpiredJwtException -> logger.error("Expired JWT token", exception)
            is UnsupportedJwtException -> logger.error("Unsupported JWT token", exception)
            is IllegalArgumentException -> logger.error("JWT claims string is empty", exception)
        }
    }.getOrDefault(false)

    fun generateToken(userDetails: UserDetailsImpl, isRefreshToken: Boolean = false): String =
        doGenerateToken(
            mapOf("role" to listOf(userDetails.getUserRole())),
            userDetails.username,
            if (isRefreshToken) jwtRefreshExpirationMs.toLong() else jwtExpirationMs.toLong()
        )

    fun getUsernameFromToken(token: String): String = runCatching {
        getAllClaimsFromToken(token).subject
    }.getOrElse { ex ->
        logger.error("Failed to extract username from token", ex)
        throw InvalidTokenException("Invalid token")
    }

    private fun doGenerateToken(claims: Map<String, Any>, username: String, expirationTime: Long): String {
        val createdDate = LocalDateTime.now()
        val expirationDate = createdDate.plusSeconds(expirationTime)
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(Date.from(createdDate.atZone(ZoneId.systemDefault()).toInstant()))
            .setExpiration(Date.from(expirationDate.atZone(ZoneId.systemDefault()).toInstant()))
            .signWith(key.value)
            .compact()
    }

    private fun getAllClaimsFromToken(token: String?): Claims =
        Jwts.parserBuilder().setSigningKey(key.value).build().parseClaimsJws(token).body


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
