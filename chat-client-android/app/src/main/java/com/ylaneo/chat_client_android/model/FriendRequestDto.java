package com.ylaneo.chat_client_android.model;

import java.time.LocalDateTime;

/**
 * 好友申请 DTO（与桌面端 FriendRequestDto 对应）
 *
 * 用于显示在"收到的好友申请"列表里，已经包含了发起人的信息
 */
public class FriendRequestDto {
    private Integer requestId;
    private Integer senderId;
    private String senderUsername;
    private String senderNickname;
    private String senderAvatar;
    private String message;
    private Integer status; // 0=待处理 1=同意 2=拒绝
    private LocalDateTime createTime;

    public FriendRequestDto() {
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 列表显示用
     */
    public String getSenderDisplayName() {
        if (senderNickname != null && !senderNickname.isEmpty()) {
            return senderNickname;
        }
        return senderUsername != null ? senderUsername : "未知用户";
    }

    /**
     * 列表显示用：displayName + @senderUsername（同名时也照常拼接）
     */
    public String getSenderDisplayNameWithAt() {
        String name = getSenderDisplayName();
        if (senderUsername != null && !senderUsername.isEmpty()) {
            return name + "   @" + senderUsername;
        }
        return name;
    }
}
