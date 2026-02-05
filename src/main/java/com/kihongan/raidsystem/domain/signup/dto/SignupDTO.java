package com.kihongan.raidsystem.domain.signup.dto;

import com.kihongan.raidsystem.domain.signup.SignupWithDetails;

/**
 * Response DTO for signup data with character and user information.
 */
public class SignupDTO {
    
    private Long id;
    private Long characterId;
    private String characterName;
    private String job;
    private Integer level;
    private Long userId;
    private String userName;
    private String userPicture;
    private String status;
    
    public SignupDTO() {
    }
    
    public SignupDTO(Long id, Long characterId, String characterName, String job, 
                    Integer level, Long userId, String userName, String userPicture, String status) {
        this.id = id;
        this.characterId = characterId;
        this.characterName = characterName;
        this.job = job;
        this.level = level;
        this.userId = userId;
        this.userName = userName;
        this.userPicture = userPicture;
        this.status = status;
    }
    
    /**
     * Creates a SignupDTO from SignupWithDetails.
     */
    public static SignupDTO fromDetails(SignupWithDetails details) {
        return new SignupDTO(
                details.getSignupId(),
                details.getCharacterId(),
                details.getCharacterName(),
                details.getJob(),
                details.getLevel(),
                details.getUserId(),
                details.getUserName(),
                details.getUserPicture(),
                details.getStatus()
        );
    }
    
    // Getters and Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCharacterId() {
        return characterId;
    }
    
    public void setCharacterId(Long characterId) {
        this.characterId = characterId;
    }
    
    public String getCharacterName() {
        return characterName;
    }
    
    public void setCharacterName(String characterName) {
        this.characterName = characterName;
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
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserPicture() {
        return userPicture;
    }
    
    public void setUserPicture(String userPicture) {
        this.userPicture = userPicture;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
