package com.ylaneo.chatserverweb.service.impl;

import com.ylaneo.chatserverweb.dao.ContactDao;
import com.ylaneo.chatserverweb.dto.FriendRequestDto;
import com.ylaneo.chatserverweb.dto.WebSocketMessage;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.service.ContactService;
import com.ylaneo.chatserverweb.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactDao contactDao;

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Override
    public Result<List<User>> getFriendList(Integer currentUserId) {
        return contactDao.getFriendList(currentUserId);
    }

    @Override
    public Result<List<User>> searchUserByKeyword(String keyword, Integer currentUserId) {
        return contactDao.searchUserByKeyword(keyword, currentUserId);
    }

    @Override
    public Result sendFriendRequest(Integer senderId, Integer receiverId, String message) {
        Result result = contactDao.sendFriendRequest(senderId, receiverId, message);
        if (result.getCode() == 0) {
            WebSocketMessage ws = new WebSocketMessage();
            ws.setType("friendRequest");
            ws.setSenderId(senderId);
            ws.setReceiverId(receiverId);
            ws.setContent(message);
            webSocketHandler.pushToUser(receiverId, ws);
        }
        return result;
    }

    @Override
    public Result<List<FriendRequestDto>> getFriendRequests(Integer receiverId) {
        return contactDao.getFriendRequests(receiverId);
    }

    @Override
    public Result handleRequest(Integer friendRequestId, Integer senderId, Integer receiverId, Integer status) {
        Result result = contactDao.handleRequest(friendRequestId, senderId, receiverId, status);
        // ★ 同意时实时推送双方好友列表更新
        if (result.getCode() == 0 && status == 1) {
            WebSocketMessage ws = new WebSocketMessage();
            ws.setType("friendListUpdate");
            webSocketHandler.pushToUser(senderId, ws);
            webSocketHandler.pushToUser(receiverId, ws);
        }
        return result;
    }

    @Override
    public Result<Integer> isFriend(Integer userId, Integer friendId) {
        return contactDao.isFriend(userId, friendId);
    }

    @Override
    public Result<Boolean> hasPendingRequest(Integer senderId, Integer receiverId) {
        return contactDao.hasPendingRequest(senderId, receiverId);
    }

    @Override
    public Result<List<Integer>> getFriendIds(Integer userId) {
        return contactDao.getFriendIds(userId);
    }
}