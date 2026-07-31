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
    //处理好友申请，根据申请的id和要修改的状态代码
    String handleRequest(String request);
    //检测两者是否已经是好友,0-不是好友；1-是好友
    String isFriend(String request);
    //检查是否已有待处理（status=0）的好友申请（注意需要双向验证，即需要调用两次）0-没有；1-有
    String hasPendingRequest(String request);
}
