package com.kihongan.raidsystem.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to inject authenticated user ID into controller method parameters.
 * 
 * Usage:
 * <pre>
 * {@code
 * @GetMapping("/me/characters")
 * public ResponseEntity<List<CharacterDTO>> getMyCharacters(@AuthUser Long userId) {
 *     // userId is automatically extracted from JWT token
 * }
 * }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}
