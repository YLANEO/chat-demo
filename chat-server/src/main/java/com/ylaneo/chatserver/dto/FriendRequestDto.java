package com.ylaneo.chatserver.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestDto {
    //
    private Integer requestId;
    private Integer senderId;
    private String senderUsername;
    private String senderNickname;
    private String senderAvatar;
    private String message;
    private Integer status;//申请状态（0=待处理，1=同意，2=拒绝）
    private LocalDateTime createTime;
}
