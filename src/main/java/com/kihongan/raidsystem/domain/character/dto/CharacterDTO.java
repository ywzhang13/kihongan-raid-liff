package com.kihongan.raidsystem.domain.character.dto;

import com.kihongan.raidsystem.domain.character.Character;

import java.time.Instant;

/**
 * Response DTO for character data.
 */
public class CharacterDTO {
    
    private Long id;
    private String name;
    private String job;
    private Integer level;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
    
    public CharacterDTO() {
    }
    
    public CharacterDTO(Long id, String name, String job, Integer level, 
                       Boolean isDefault, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.job = job;
        this.level = level;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Creates a CharacterDTO from a Character entity.
     */
    public static CharacterDTO fromEntity(Character character) {
        return new CharacterDTO(
                character.getId(),
                character.getName(),
                character.getJob(),
                character.getLevel(),
                character.getIsDefault(),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
