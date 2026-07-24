package com.ylaneo.chatserver.dao;

import com.ylaneo.chatserver.entity.FriendRequest;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;

import java.util.List;

public interface ContactDao {
    //获得当前用户的所有好友的列表
    Result<List<User>> getFriendList(Integer currentUserId);
    // 根据昵称模糊搜索 或 用户名搜索
    Result<List<User>> searchUserByKeyword(String keyword, Integer currentUserId);
    //发送好友申请
    Result sendFriendRequest(Integer senderId, Integer receiverId, String message);
    //获取当前用户的待处理的好友申请列表
    Result<List<FriendRequest>> getFriendRequests(Integer receiverId);
}
