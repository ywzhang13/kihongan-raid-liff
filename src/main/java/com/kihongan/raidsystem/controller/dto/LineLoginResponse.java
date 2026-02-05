package com.kihongan.raidsystem.controller.dto;

public class LineLoginResponse {
    private String appToken;
    private String lineUserId;
    private Long userDbId;

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getLineUserId() {
        return lineUserId;
    }

    public void setLineUserId(String lineUserId) {
        this.lineUserId = lineUserId;
    }

    public Long getUserDbId() {
        return userDbId;
    }

    public void setUserDbId(Long userDbId) {
        this.userDbId = userDbId;
    }
}
