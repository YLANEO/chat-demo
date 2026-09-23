package com.ylaneo.chatserverweb.controller;

import com.ylaneo.chatserverweb.dto.FriendRequestDto;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.service.ContactService;
import com.ylaneo.chatserverweb.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contact")
//@CrossOrigin
public class ContactController {

    @Autowired
    private ContactService contactService;

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("userid");
    }

    //获取所有的好友列表
    @GetMapping("/friends")
    public Result<List<User>> friends() {
        return contactService.getFriendList(currentUserId());
    }

    //根据关键词搜索用户的好友
    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) {
        return contactService.searchUserByKeyword(keyword, currentUserId());
    }

    //发送好友申请，依旧不需要传当前用户的id，因为可以直接获取
    @PostMapping("/request/send")
    public Result sendRequest(@RequestBody Map<String, Object> body) {
        Integer receiverId = (Integer) body.get("receiverId");
        String message = (String) body.getOrDefault("message", "请求添加好友");
        return contactService.sendFriendRequest(currentUserId(), receiverId, message);
    }

    //获取当前用户的所有的好友申请列表
    @GetMapping("/requests")
    public Result<List<FriendRequestDto>> requests() {
        return contactService.getFriendRequests(currentUserId());
    }

    //处理好友申请
    @PostMapping("/request/handle")
    public Result handleRequest(@RequestBody Map<String, Object> body) {
        Integer requestId=(Integer)body.get("requestId");
        Integer senderId=(Integer)body.get("senderId");
        Integer status=(Integer)body.get("status");
        return contactService.handleRequest(requestId, senderId, currentUserId(), status);
    }

    //检测两人是否为好友
    @GetMapping("/is-friend")
    public Result<Integer> isFriend(@RequestParam Integer userId, @RequestParam Integer friendId) {
        return contactService.isFriend(userId, friendId);
    }

    //检测是否已有待处理的好友申请
    @GetMapping("/has-pending-request")
    public Result<Boolean> hasPendingRequest(@RequestParam Integer senderId, @RequestParam Integer receiverId) {
        return contactService.hasPendingRequest(senderId, receiverId);
    }
}