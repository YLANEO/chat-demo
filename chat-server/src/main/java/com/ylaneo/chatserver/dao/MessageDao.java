package com.ylaneo.chatserver.dao;

import com.ylaneo.chatserver.entity.ChatMessage;
import com.ylaneo.chatserver.entity.Result;

import java.util.List;

public interface MessageDao {
    //根据用户和好友的Id获得所有的聊天消息
    Result<List<ChatMessage>>getAllMessage(Integer userId, Integer friendId);
    //发送消息给好友
    Result<ChatMessage> sendMessage(Integer senderId,Integer receiverId,Integer type,String content,Integer status);
    //根据id查找一条消息
    Result<ChatMessage> findMessageById(Integer messageId);
}
