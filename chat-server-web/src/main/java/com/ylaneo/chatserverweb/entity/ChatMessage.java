package com.ylaneo.chatserverweb.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
