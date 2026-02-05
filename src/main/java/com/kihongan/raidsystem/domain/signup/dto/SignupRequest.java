package com.kihongan.raidsystem.domain.signup.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a signup.
 */
public class SignupRequest {
    
    @NotNull(message = "Character ID is required")
    private Long characterId;
    
    public SignupRequest() {
    }
    
    public SignupRequest(Long characterId) {
        this.characterId = characterId;
    }
    
    // Getters and Setters
    
    public Long getCharacterId() {
        return characterId;
    }
    
    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }
}
