package com.ylaneo.chatserverweb.service;



import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.Result;

import java.util.List;

public interface MessageService {
    //根据用户和好友的Id获得所有的聊天消息
    Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId);
    //发送消息给好友
    Result<ChatMessage> sendMessage(Integer senderId,Integer receiverId,Integer type,String content,Integer status);
    //根据id查找一条消息
    Result<ChatMessage> findMessageById(Integer messageId);
    //分页查询历史消息（用于"加载更多"）
    Result<List<ChatMessage>> getHistoryMessage(Integer userId, Integer friendId, Long lastMessageId, Integer limit);
    //批量标记消息为已读
    Result<Integer> markAsRead(Integer userId, Integer friendId);
    //统计两人之间未读消息总数
    Result<Integer> countUnread(Integer userId, Integer friendId);
    //根据当前用户统计所有未读消息（好友列表上红点总数）
    Result<Integer> countTotalUnread(Integer userId);
}
