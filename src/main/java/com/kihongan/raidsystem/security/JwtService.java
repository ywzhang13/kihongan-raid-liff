package com.kihongan.raidsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Service for JWT token validation and claims extraction.
 * Handles token signature verification, expiration checks, and user identity extraction.
 */
@Service
public class JwtService {
    
    private final SecretKey secretKey;
    
    public JwtService(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Validates JWT token signature and expiration.
     * 
     * @param token JWT token string
     * @return Claims object containing token payload
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    /**
     * Extracts user ID from JWT claims.
     * 
     * @param claims JWT claims
     * @return User ID (database primary key)
     */
    public Long extractUserId(Claims claims) {
        Object sub = claims.get("sub");
        if (sub instanceof Integer) {
            return ((Integer) sub).longValue();
        } else if (sub instanceof Long) {
            return (Long) sub;
        } else if (sub instanceof String) {
            return Long.parseLong((String) sub);
        }
        throw new IllegalArgumentException("Invalid user ID in token");
    }
    
    /**
     * Extracts LINE user ID from JWT claims.
     * 
     * @param claims JWT claims
     * @return LINE user ID string
     */
    public String extractLineUserId(Claims claims) {
        return claims.get("lineUserId", String.class);
    }
    
    /**
     * Generates a JWT token for a user (for testing purposes).
     * 
     * @param userId Database user ID
     * @param lineUserId LINE user ID
     * @param expirationMs Expiration time in milliseconds
     * @return JWT token string
     */
    public String generateToken(Long userId, String lineUserId, long expirationMs) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);
        
        return Jwts.builder()
                .subject(userId.toString())
                .claim("lineUserId", lineUserId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }
}
