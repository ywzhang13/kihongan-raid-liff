package com.kihongan.raidsystem.domain.raid.dto;

import com.kihongan.raidsystem.domain.raid.Raid;

import java.time.Instant;

/**
 * Response DTO for raid data.
 */
public class RaidDTO {
    
    private Long id;
    private String title;
    private String subtitle;
    private String boss;
    private Instant startTime;
    private Long createdBy;
    private String createdByName;
    private Instant createdAt;
    
    public RaidDTO() {
    }
    
    public RaidDTO(Long id, String title, String subtitle, String boss, 
                  Instant startTime, Long createdBy, String createdByName, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.boss = boss;
        this.startTime = startTime;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
    }
    
    /**
     * Creates a RaidDTO from a Raid entity.
     */
    public static RaidDTO fromEntity(Raid raid) {
        return new RaidDTO(
                raid.getId(),
                raid.getTitle(),
                raid.getSubtitle(),
                raid.getBoss(),
                raid.getStartTime(),
                raid.getCreatedBy(),
                null, // createdByName will be set separately
                raid.getCreatedAt()
        );
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public Long getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getCreatedByName() {
        return createdByName;
    }
    
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
