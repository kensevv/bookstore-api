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
    companion object {
        const val ACCESS_TOKEN_TYPE = "token"
        const val REFRESH_TOKEN_TYPE = "refresh_token"
    }

    private val logger = KotlinLogging.logger {}

    private val key = lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    fun validateToken(token: String, isRefreshToken: Boolean = false): Boolean = runCatching {
        val tokenType = if (isRefreshToken) REFRESH_TOKEN_TYPE else ACCESS_TOKEN_TYPE
        !isTokenExpired(token) && getTokenTypeFromToken(token) == tokenType
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
            userDetails,
            if (isRefreshToken) jwtRefreshExpirationMs.toLong() else jwtExpirationMs.toLong(),
            if (isRefreshToken) REFRESH_TOKEN_TYPE else ACCESS_TOKEN_TYPE
        )

    fun getEmailFromToken(token: String): String = runCatching {
        getAllClaimsFromToken(token).subject
    }.getOrElse { ex ->
        logger.error("Failed to extract email from token", ex)
        throw InvalidTokenException("Invalid token")
    }

    private fun doGenerateToken(userDetails: UserDetailsImpl, expirationTime: Long, tokenType: String): String {
        val createdDate = LocalDateTime.now()
        val expirationDate = createdDate.plusSeconds(expirationTime)
        val claims = mapOf(
            "role" to listOf(userDetails.getUserRole()),
            "tokenType" to tokenType,
        )
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.username)
            .setIssuedAt(Date.from(createdDate.atZone(ZoneId.systemDefault()).toInstant()))
            .setExpiration(Date.from(expirationDate.atZone(ZoneId.systemDefault()).toInstant()))
            .signWith(key.value)
            .compact()
    }

    private fun getAllClaimsFromToken(token: String?): Claims =
        Jwts.parserBuilder().setSigningKey(key.value).build().parseClaimsJws(token).body

    fun getTokenTypeFromToken(token: String): String {
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
