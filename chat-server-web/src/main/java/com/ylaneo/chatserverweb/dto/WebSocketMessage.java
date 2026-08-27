package com.ylaneo.chatserverweb.dto;

import lombok.Data;

@Data
public class WebSocketMessage {
    private String type;       // 消息类型: chat / statusUpdate / friendRequest
    private Integer senderId;   // 发送者 ID
    private Integer receiverId; // 接收者 ID（私聊时使用）
    private Object content;     // 消息内容
    private Object data;        // 扩展数据
}