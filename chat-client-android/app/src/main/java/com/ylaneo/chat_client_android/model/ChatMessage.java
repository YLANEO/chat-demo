package com.ylaneo.chat_client_android.model;

import java.time.LocalDateTime;

/**
 * 聊天消息实体（与桌面端 ChatMessage 对应）
 *
 * 字段：
 *   chatMessageId  消息 ID
 *   senderId       发送者 ID
 *   receiverId     接收者 ID
 *   type           消息类型（0=文本）
 *   content        内容
 *   readAt         已读时间（可为 null）
 *   status         状态：0=未读 1=已读
 *   createTime     创建时间
 *   updateTime     更新时间
 */
public class ChatMessage {
    private Integer chatMessageId;
    private Integer senderId;
    private Integer receiverId;
    private Integer type;
    private String content;
    private LocalDateTime readAt;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public ChatMessage() {
    }

    public Integer getChatMessageId() {
        return chatMessageId;
    }

    public void setChatMessageId(Integer chatMessageId) {
        this.chatMessageId = chatMessageId;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public Integer getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Integer receiverId) {
        this.receiverId = receiverId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
