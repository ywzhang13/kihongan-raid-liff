package com.kihongan.raidsystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 * Tests authentication filter behavior with valid, invalid, and missing tokens.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    
    private static final String TEST_SECRET = "test-secret-key-for-testing-must-be-at-least-32-characters";
    
    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);
        filter = new JwtAuthenticationFilter(jwtService);
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void requestWithoutTokenReturns401() throws Exception {
        // GIVEN a request without Authorization header
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // WHEN the filter processes the request
        filter.doFilterInternal(request, response, filterChain);
        
        // THEN the filter chain should continue (no authentication set)
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
    
    @Test
    void requestWithInvalidTokenReturns401() throws Exception {
        // GIVEN a request with invalid token
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid.token.here");
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // WHEN the filter processes the request
        filter.doFilterInternal(request, response, filterChain);
        
        // THEN it should return 401 Unauthorized
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertThat(stringWriter.toString()).contains("Unauthorized");
        assertThat(stringWriter.toString()).contains("Invalid or expired token");
    }
    
    @Test
    void requestWithValidTokenPassesThrough() throws Exception {
        // GIVEN a request with valid token
        Long userId = 123L;
        String lineUserId = "U1234567890";
        String validToken = jwtService.generateToken(userId, lineUserId, 3600000);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        
        // WHEN the filter processes the request
        filter.doFilterInternal(request, response, filterChain);
        
        // THEN the filter chain should continue
        verify(filterChain).doFilter(request, response);
        
        // AND authentication should be set in SecurityContext
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal).isInstanceOf(JwtAuthenticationFilter.UserAuthentication.class);
        
        JwtAuthenticationFilter.UserAuthentication userAuth = 
                (JwtAuthenticationFilter.UserAuthentication) principal;
        assertThat(userAuth.getUserId()).isEqualTo(userId);
        assertThat(userAuth.getLineUserId()).isEqualTo(lineUserId);
    }
    
    @Test
    void requestWithExpiredTokenReturns401() throws Exception {
        // GIVEN a request with expired token
        Long userId = 123L;
        String lineUserId = "U1234567890";
        String expiredToken = jwtService.generateToken(userId, lineUserId, -1000);
        
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        
        // WHEN the filter processes the request
        filter.doFilterInternal(request, response, filterChain);
        
        // THEN it should return 401 Unauthorized
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertThat(stringWriter.toString()).contains("Unauthorized");
    }
    
    @Test
    void requestWithMalformedAuthorizationHeaderPassesThrough() throws Exception {
        // GIVEN a request with malformed Authorization header (no "Bearer " prefix)
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token");
        
        // WHEN the filter processes the request
        filter.doFilterInternal(request, response, filterChain);
        
        // THEN the filter chain should continue (no authentication set)
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
