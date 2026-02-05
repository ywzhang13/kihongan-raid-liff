package com.kihongan.raidsystem.domain.character.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new character.
 */
public class CreateCharacterRequest {
    
    @NotBlank(message = "Name cannot be empty")
    private String name;
    
    private String job;
    private Integer level;
    private Boolean isDefault;
    
    public CreateCharacterRequest() {
    }
    
    public CreateCharacterRequest(String name, String job, Integer level, Boolean isDefault) {
        this.name = name;
        this.job = job;
        this.level = level;
        this.isDefault = isDefault;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getJob() {
        return job;
    }
    
    public void setJob(String job) {
        this.job = job;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public Boolean getIsDefault() {
        return isDefault;
    }
    
    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
