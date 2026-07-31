package com.ylaneo.chatserver.dao;

import com.ylaneo.chatserver.dto.FriendRequestDto;
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
    Result<List<FriendRequestDto>> getFriendRequests(Integer receiverId);
    //处理好友申请,根据申请的id和要修改的状态代码
    Result handleRequest(Integer friendRequestId, Integer senderId, Integer receiverId, Integer status);
    //检测两者是否已经是好友,0-不是好友；1-是好友
    Result<Integer> isFriend(Integer userId,Integer friendId);
    //检查是否已有待处理（status=0）的好友申请（注意需要双向验证，即需要调用两次）0-没有；1-有
    public Result<Boolean> hasPendingRequest(Integer senderId, Integer receiverId);
}
