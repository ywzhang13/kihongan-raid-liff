package com.kihongan.raidsystem.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter that validates Bearer tokens and sets authentication context.
 * Extracts JWT from Authorization header, validates it, and populates SecurityContext.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract Bearer token from Authorization header
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                // Validate token and extract claims
                Claims claims = jwtService.validateToken(token);
                Long userId = jwtService.extractUserId(claims);
                String lineUserId = jwtService.extractLineUserId(claims);
                
                // Create authentication object with userId as principal
                UserAuthentication userAuth = new UserAuthentication(userId, lineUserId);
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                userAuth, 
                                null, 
                                Collections.emptyList()
                        );
                
                // Set authentication in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } catch (JwtException e) {
            // Handle authentication errors with 401 response
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or expired token\"}");
        }
    }
    
    /**
     * Extracts JWT token from Authorization header.
     * 
     * @param request HTTP request
     * @return JWT token string or null if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        return null;
    }
    
    /**
     * Simple authentication object to hold user identity.
     */
    public static class UserAuthentication {
        private final Long userId;
        private final String lineUserId;
        
        public UserAuthentication(Long userId, String lineUserId) {
            this.userId = userId;
            this.lineUserId = lineUserId;
        }
        
        public Long getUserId() {
            return userId;
        }
        
        public String getLineUserId() {
            return lineUserId;
        }
    }
}
