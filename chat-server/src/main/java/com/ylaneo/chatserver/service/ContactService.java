package com.ylaneo.chatserver.service;

import com.ylaneo.chatserver.dto.FriendRequestDto;
import com.ylaneo.chatserver.entity.Result;

import java.util.List;

public interface ContactService {
    //获取所有好友列表信息
    String getFriendList(String request);
    //根据昵称模糊搜索或用户名搜索
    String searchUserByKeyword(String request);
    //发送好友申请
    String sendFriendRequest(String request);
    //获取当前用户的待处理的好友申请列表
    String getFriendRequests(String request);
}
