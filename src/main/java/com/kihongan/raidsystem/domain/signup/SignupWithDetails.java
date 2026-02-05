package com.kihongan.raidsystem.domain.signup;

/**
 * Signup with complete character and user information.
 * Used for query results with JOIN.
 */
public class SignupWithDetails {
    private Long signupId;
    private Long characterId;
    private String characterName;
    private String job;
    private Integer level;
    private Long userId;
    private String userName;
    private String userPicture;
    private String status;
    
    public SignupWithDetails() {
    }
    
    public SignupWithDetails(Long signupId, Long characterId, String characterName, 
                            String job, Integer level, Long userId, String userName, 
                            String userPicture, String status) {
        this.signupId = signupId;
        this.characterId = characterId;
        this.characterName = characterName;
        this.job = job;
        this.level = level;
        this.userId = userId;
        this.userName = userName;
        this.userPicture = userPicture;
        this.status = status;
    }
    
    // Getters and Setters
    
    public Long getSignupId() {
        return signupId;
    }
    
    public void setSignupId(Long signupId) {
        this.signupId = signupId;
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
