package com.ylaneo.chatserverweb.service.impl;

import com.ylaneo.chatserverweb.dao.MessageDao;
import com.ylaneo.chatserverweb.dto.WebSocketMessage;
import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.gateway.LegacyPushNotifier;
import com.ylaneo.chatserverweb.gateway.LegacyResponseBuilder;
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

    @Autowired
    private LegacyPushNotifier pushNotifier;

    @Autowired
    private LegacyResponseBuilder responseBuilder;

    @Override
    public Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId) {
        return messageDao.getAllMessage(userId, friendId);
    }
    /**
     * 发送消息：
     *   1. 落库
     *   2. 推 WebSocket + swing 通知接收方（receiver）
     *   3. 推 WebSocket + swing 通知发送方（sender）：多端同步，sender 的其他设备也能收到这条消息
     */
    @Override
    public Result<ChatMessage> sendMessage(Integer senderId, Integer receiverId, Integer type, String content, Integer status) {
        Result<ChatMessage> result = messageDao.sendMessage(senderId, receiverId, type, content, status);
        if (result.getCode() == 0 && result.getData() != null) {
            Integer messageId = result.getData().getChatMessageId();

            // 构建推送消息（web + swing 共用）
            WebSocketMessage ws = new WebSocketMessage();
            ws.setType("chat");
            ws.setSenderId(senderId);
            ws.setReceiverId(receiverId);
            ws.setContent(result.getData());
            String legacyPush = responseBuilder.buildPushMessageAdd(senderId, messageId);

            // 推给接收方（receiver）：通知对方有新消息
            webSocketHandler.pushToUser(receiverId, ws);
            pushNotifier.pushTo(receiverId, legacyPush);

            // 推给发送方（sender）：让sender的其他设备也能同步看到这条消息。这里容易遗漏（容易只考虑了一端）
            webSocketHandler.pushToUser(senderId, ws);
            pushNotifier.pushTo(senderId, legacyPush);
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
     *   2. 推WebSocket告诉对方"你的消息已被对方读了"
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
