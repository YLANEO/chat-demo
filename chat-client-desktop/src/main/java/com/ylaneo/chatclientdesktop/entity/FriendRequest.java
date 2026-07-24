package com.ylaneo.chatclientdesktop.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {
    private Integer requestId;
    private Integer senderId;
    private Integer receriverId;
    private String message;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
