package com.ylaneo.chatserver.dao;

import com.ylaneo.chatserver.entity.ChatMessage;
import com.ylaneo.chatserver.entity.Result;

import java.util.List;

public interface MessageDao {
    //根据用户和好友的Id获得所有的聊天消息
    Result<List<ChatMessage>>getAllMessage(Integer userId, Integer friendId);
}
