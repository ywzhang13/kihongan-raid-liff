package com.kihongan.raidsystem.domain.raid;

import java.time.Instant;

/**
 * Raid entity representing a scheduled game event.
 */
public class Raid {
    private Long id;
    private String title;
    private String subtitle;
    private String boss;
    private Instant startTime;
    private Long createdBy;
    private Instant createdAt;
    
    public Raid() {
    }
    
    public Raid(Long id, String title, String subtitle, String boss, 
               Instant startTime, Long createdBy, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.boss = boss;
        this.startTime = startTime;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
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
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
