package com.chatbot.security

import com.chatbot.domain.user.entity.Role
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    // PRODUCTION_TODO: Reduce to 1h and implement refresh token rotation
    @Value("\${jwt.expiration}") private val expirationMs: Long
) {
    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun createToken(userId: Long, role: Role): String {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expirationMs))
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
            true
        } catch (e: JwtException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).payload
        return claims.subject.toLong()
    }
}
