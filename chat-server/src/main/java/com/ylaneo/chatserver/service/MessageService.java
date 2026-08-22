package com.ylaneo.chatserver.service;

import com.ylaneo.chatserver.entity.ChatMessage;
import com.ylaneo.chatserver.entity.Result;

public interface MessageService {
    //获取所有消息列表信息
    String getAllMessage(String request);
    //发送消息给好友
    String sendMessage(String request);
    //根据id查找一条消息
    String findMessageById(String request);
}
