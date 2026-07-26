package com.ylaneo.chatclientdesktop.facade;

import com.ylaneo.chatclientdesktop.dto.FriendRequestDto;
import com.ylaneo.chatclientdesktop.entity.FriendRequest;
import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;

import java.util.List;

public interface ContactFacade {
    //获得所有好友的列表
    Result<List<User>> getFriendList(Integer currentUserId);
    // 根据昵称模糊搜索 或 用户名搜索
    Result<List<User>> searchUserByKeyword(String keyword, Integer currentUserId);
    //发送好友申请
    Result sendFriendRequest(Integer senderId,Integer receiverId,String message);
    //获取当前用户的待处理的好友申请列表
    Result<List<FriendRequestDto>> getFriendRequests(Integer receiverId);
    //处理好友申请
//    Result
}
