package com.ylaneo.chatclientdesktop.facade;

import com.ylaneo.chatclientdesktop.entity.ChatMessage;
import com.ylaneo.chatclientdesktop.entity.Result;

import java.util.List;

public interface MessageFacade {
    //根据用户和好友的Id获得所有的聊天消息
    Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId);
}
