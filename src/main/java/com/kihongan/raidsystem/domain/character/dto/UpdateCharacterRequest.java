package com.kihongan.raidsystem.domain.character.dto;

/**
 * Request DTO for updating an existing character.
 * All fields are optional to support partial updates.
 */
public class UpdateCharacterRequest {
    
    private String name;
    private String job;
    private Integer level;
    private Boolean isDefault;
    
    public UpdateCharacterRequest() {
    }
    
    public UpdateCharacterRequest(String name, String job, Integer level, Boolean isDefault) {
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
