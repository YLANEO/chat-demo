package com.ylaneo.chatserverweb.service.impl;

import com.ylaneo.chatserverweb.dto.FriendRequestDto;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.service.ContactService;

import java.util.List;

public class ContactServiceImpl implements ContactService {
    @Override
    public Result<List<User>> getFriendList(Integer currentUserId) {
        return null;
    }

    @Override
    public Result<List<User>> searchUserByKeyword(String keyword, Integer currentUserId) {
        return null;
    }

    @Override
    public Result sendFriendRequest(Integer senderId, Integer receiverId, String message) {
        return null;
    }

    @Override
    public Result<List<FriendRequestDto>> getFriendRequests(Integer receiverId) {
        return null;
    }

    @Override
    public Result handleRequest(Integer friendRequestId, Integer senderId, Integer receiverId, Integer status) {
        return null;
    }

    @Override
    public Result<Integer> isFriend(Integer userId, Integer friendId) {
        return null;
    }

    @Override
    public Result<Boolean> hasPendingRequest(Integer senderId, Integer receiverId) {
        return null;
    }

    @Override
    public Result<List<Integer>> getFriendIds(Integer userId) {
        return null;
    }
}
