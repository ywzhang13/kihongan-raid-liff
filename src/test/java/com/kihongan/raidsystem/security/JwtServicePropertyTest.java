package com.kihongan.raidsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.BeforeEach;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for JwtService.
 * Tests JWT token validation and claims extraction with random inputs.
 */
class JwtServicePropertyTest {
    
    private JwtService jwtService;
    private static final String TEST_SECRET = "test-secret-key-for-testing-must-be-at-least-32-characters";
    
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);
    }
    
    // Feature: kihongan-raid-system, Property 24: Invalid tokens rejected
    @Property(tries = 100)
    void invalidTokensAreRejected(@ForAll("invalidTokens") String invalidToken) {
        // WHEN attempting to validate an invalid token
        // THEN it should throw JwtException
        assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
                .isInstanceOf(JwtException.class);
    }
    
    // Feature: kihongan-raid-system, Property 25: Token claims extraction
    @Property(tries = 100)
    void validTokenClaimsCanBeExtracted(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String lineUserId) {
        
        // GIVEN a valid token with userId and lineUserId
        String token = jwtService.generateToken(userId, lineUserId, 3600000);
        
        // WHEN validating the token
        Claims claims = jwtService.validateToken(token);
        
        // THEN the extracted userId and lineUserId should match
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtService.extractLineUserId(claims)).isEqualTo(lineUserId);
    }
    
    @Property(tries = 100)
    void expiredTokensAreRejected(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String lineUserId) {
        
        // GIVEN an expired token (expiration = -1000ms, i.e., 1 second ago)
        String expiredToken = jwtService.generateToken(userId, lineUserId, -1000);
        
        // WHEN attempting to validate the expired token
        // THEN it should throw JwtException
        assertThatThrownBy(() -> jwtService.validateToken(expiredToken))
                .isInstanceOf(JwtException.class);
    }
    
    @Property(tries = 100)
    void tokenWithWrongSignatureIsRejected(
            @ForAll @LongRange(min = 1, max = 1000000) Long userId,
            @ForAll @AlphaChars @StringLength(min = 10, max = 50) String lineUserId) {
        
        // GIVEN a token signed with a different secret
        String differentSecret = "different-secret-key-must-be-at-least-32-characters-long";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));
        
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 3600000);
        
        String tokenWithWrongSignature = Jwts.builder()
                .subject(userId.toString())
                .claim("lineUserId", lineUserId)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(differentKey)
                .compact();
        
        // WHEN attempting to validate the token with wrong signature
        // THEN it should throw JwtException
        assertThatThrownBy(() -> jwtService.validateToken(tokenWithWrongSignature))
                .isInstanceOf(JwtException.class);
    }
    
    // Arbitraries for generating test data
    
    @Provide
    Arbitrary<String> invalidTokens() {
        return Arbitraries.oneOf(
                Arbitraries.just(""),
                Arbitraries.just("invalid.token.string"),
                Arbitraries.just("not-a-jwt"),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100),
                Arbitraries.just("eyJhbGciOiJIUzI1NiJ9.invalid.signature")
        );
    }
}
