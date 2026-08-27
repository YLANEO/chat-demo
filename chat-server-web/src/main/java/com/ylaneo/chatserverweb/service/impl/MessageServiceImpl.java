package com.ylaneo.chatserverweb.service.impl;

import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.service.MessageService;

import java.util.List;

public class MessageServiceImpl implements MessageService {
    @Override
    public Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId) {
        return null;
    }

    @Override
    public Result<ChatMessage> sendMessage(Integer senderId, Integer receiverId, Integer type, String content, Integer status) {
        return null;
    }

    @Override
    public Result<ChatMessage> findMessageById(Integer messageId) {
        return null;
    }
}
