package com.kihongan.raidsystem.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves @AuthUser annotated parameters by extracting userId from SecurityContext.
 * Integrates with JwtAuthenticationFilter to provide authenticated user ID to controllers.
 */
@Component
public class AuthUserArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(AuthUser.class) 
                && parameter.getParameterType().equals(Long.class);
    }
    
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof JwtAuthenticationFilter.UserAuthentication) {
                JwtAuthenticationFilter.UserAuthentication userAuth = 
                        (JwtAuthenticationFilter.UserAuthentication) principal;
                return userAuth.getUserId();
            }
        }
        
        throw new IllegalStateException("User not authenticated");
    }
}
