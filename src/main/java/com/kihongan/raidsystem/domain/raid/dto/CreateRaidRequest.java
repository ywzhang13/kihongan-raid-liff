package com.kihongan.raidsystem.domain.raid.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for creating a new raid.
 */
public class CreateRaidRequest {
    
    @NotBlank(message = "Title cannot be empty")
    private String title;
    
    private String subtitle;
    private String boss;
    
    @NotNull(message = "Start time is required")
    private Instant startTime;
    
    private Long characterId;
    
    public CreateRaidRequest() {
    }
    
    public CreateRaidRequest(String title, String subtitle, String boss, Instant startTime, Long characterId) {
        this.title = title;
        this.subtitle = subtitle;
        this.boss = boss;
        this.startTime = startTime;
        this.characterId = characterId;
    }
    
    // Getters and Setters
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSubtitle() {
        return subtitle;
    }
    
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
    
    public String getBoss() {
        return boss;
    }
    
    public void setBoss(String boss) {
        this.boss = boss;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Long getCharacterId() {
        return characterId;
    }
    
    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }
}
