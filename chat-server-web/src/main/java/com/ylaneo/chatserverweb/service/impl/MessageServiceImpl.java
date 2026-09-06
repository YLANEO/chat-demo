package com.ylaneo.chatserverweb.service.impl;

import com.ylaneo.chatserverweb.dao.MessageDao;
import com.ylaneo.chatserverweb.dto.WebSocketMessage;
import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.service.MessageService;
import com.ylaneo.chatserverweb.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageDao messageDao;
    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Override
    public Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId) {
        return messageDao.getAllMessage(userId, friendId);
    }
    /**
     * 发送消息：
     *   1. 落库
     *   2. 推 WebSocket 通知对方（如果对方在线）
     */
    @Override
    public Result<ChatMessage> sendMessage(Integer senderId, Integer receiverId, Integer type, String content, Integer status) {
        Result<ChatMessage> result = messageDao.sendMessage(senderId, receiverId, type, content, status);
        if (result.getCode() == 0 && result.getData() != null) {
            // 推送：告诉"接收方"你收到了一条新消息
            WebSocketMessage ws = new WebSocketMessage();
            ws.setType("chat");
            ws.setSenderId(senderId);
            ws.setReceiverId(receiverId);
            ws.setContent(result.getData());
            webSocketHandler.pushToUser(receiverId, ws);
        }
        return result;
    }

    @Override
    public Result<ChatMessage> findMessageById(Integer messageId) {
        return messageDao.findMessageById(messageId);
    }

    @Override
    public Result<List<ChatMessage>> getHistoryMessage(Integer userId, Integer friendId, Long lastMessageId, Integer limit) {
        return messageDao.getHistoryMessage(userId, friendId, lastMessageId, limit);
    }

    /**
     * 标记已读：
     *   1. 批量更新数据库
     *   2. 推 WebSocket 告诉对方"你的消息已被对方读了"
     */
    @Override
    public Result<Integer> markAsRead(Integer userId, Integer friendId) {
        Result<Integer> result = messageDao.markAsRead(userId, friendId);
        if (result.getCode() == 0) {
            WebSocketMessage ws = new WebSocketMessage();
            ws.setType("read");
            ws.setSenderId(userId);      // 我自己
            ws.setReceiverId(friendId);  // 对面那位
            ws.setData(result.getData()); // 读了几条
            webSocketHandler.pushToUser(friendId, ws);
        }
        return result;
    }
    @Override
    public Result<Integer> countUnread(Integer userId, Integer friendId) {
        return messageDao.countUnread(userId, friendId);
    }
    @Override
    public Result<Integer> countTotalUnread(Integer userId) {
        return messageDao.countTotalUnread(userId);
    }
}
