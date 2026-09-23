package com.ylaneo.chatserverweb.dto;

import lombok.Data;

@Data
public class WebSocketMessage {
    private String type;//消息类型:chat/statusUpdate/friendRequest
    private Integer senderId;
    private Integer receiverId;
    private Object content;
    private Object data;
}