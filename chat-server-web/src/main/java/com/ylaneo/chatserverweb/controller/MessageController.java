package com.ylaneo.chatserverweb.controller;

import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.service.MessageService;
import com.ylaneo.chatserverweb.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/message")
//@CrossOrigin
public class MessageController {
    @Autowired
    private MessageService messageService;

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("userid");
    }

    //所有的历史聊天信息
    @GetMapping("/history")
    public Result<List<ChatMessage>> getHistory(@RequestParam Integer friendId) {
        return messageService.getAllMessage(currentUserId(), friendId);
    }

    //获取固定数量的历史聊天消息
    @GetMapping("/page")
    public Result<List<ChatMessage>> getPage(@RequestParam Integer friendId, @RequestParam(required = false) Long lastMessageId, @RequestParam(defaultValue = "20") Integer limit) {
        return messageService.getHistoryMessage(currentUserId(), friendId, lastMessageId, limit);
    }

    //发送消息
//    @PostMapping("/send")
//    public Result<ChatMessage> send(@RequestBody Map<String, Object> body) {
//        Integer friendId = (Integer) body.get("friendId");
//        Integer type = (Integer) body.get("type");
//        String  content = (String)  body.get("content");
//        return messageService.sendMessage(currentUserId(), friendId, type, content, 0);
//    }

    @PostMapping("/send")
    public Result<ChatMessage> send(@RequestParam Integer friendId,@RequestParam Integer type,@RequestParam String content) {
        return messageService.sendMessage(currentUserId(), friendId, type, content, 0);
    }

    //将当前用户打开的好友窗口中的未读消息消息标记为已读（修改read_at字段为当前时间）
    @PostMapping("/read")
    public Result<Integer> markRead(@RequestBody Map<String, Object> body) {
        Integer friendId = (Integer) body.get("friendId");
        return messageService.markAsRead(currentUserId(), friendId);
    }

    //获取当前用户的指定好友的未读消息数量
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(@RequestParam Integer friendId) {
        return messageService.countUnread(currentUserId(), friendId);
    }

    //获取
    @GetMapping("/total-unread")
    public Result<Integer> totalUnread() {
        return messageService.countTotalUnread(currentUserId());
    }
}
