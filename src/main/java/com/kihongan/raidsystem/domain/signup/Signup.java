package com.kihongan.raidsystem.domain.signup;

import java.time.Instant;

/**
 * Signup entity representing a character's registration for a raid.
 */
public class Signup {
    private Long id;
    private Long raidId;
    private Long characterId;
    private String status;
    private Instant createdAt;
    
    public Signup() {
    }
    
    public Signup(Long id, Long raidId, Long characterId, String status, Instant createdAt) {
        this.id = id;
        this.raidId = raidId;
        this.characterId = characterId;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getRaidId() {
        return raidId;
    }
    
    public void setRaidId(Long raidId) {
        this.raidId = raidId;
    }
    
    public Long getCharacterId() {
        return characterId;
    }
    
    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
