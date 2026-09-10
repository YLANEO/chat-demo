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
@CrossOrigin
public class MessageController {
    @Autowired
    private MessageService messageService;
    /** 当前登录用户ID（从 ThreadLocal 取，LoginInterceptor 写入） */
    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return (Integer) claims.get("userid");
    }
    /**
     * 拉取与某好友的聊天记录（不带分页：直接全部）
     * GET /api/message/history?friendId=2
     */
    @GetMapping("/history")
    public Result<List<ChatMessage>> getHistory(@RequestParam Integer friendId) {
        return messageService.getAllMessage(currentUserId(), friendId);
    }
    /**
     * 分页向上滚动加载
     * GET /api/message/page?friendId=2&lastMessageId=100&limit=20
     */
    @GetMapping("/page")
    public Result<List<ChatMessage>> getPage(@RequestParam Integer friendId,
         @RequestParam(required = false) Long lastMessageId, @RequestParam(defaultValue = "20") Integer limit) {
        return messageService.getHistoryMessage(currentUserId(), friendId, lastMessageId, limit);
    }
    /**
     * 发送消息
     * POST /api/message/send  body: { friendId:2, type:0, content:"hi" }
     */
    @PostMapping("/send")
    public Result<ChatMessage> send(@RequestBody Map<String, Object> body) {
        Integer friendId = (Integer) body.get("friendId");
        Integer type     = (Integer) body.get("type");
        String  content  = (String)  body.get("content");
        return messageService.sendMessage(currentUserId(), friendId, type, content, 0);
    }
    /**
     * 标记来自某好友的消息为已读
     * POST /api/message/read  body: { friendId:2 }
     */
    @PostMapping("/read")
    public Result<Integer> markRead(@RequestBody Map<String, Object> body) {
        Integer friendId = (Integer) body.get("friendId");
        return messageService.markAsRead(currentUserId(), friendId);
    }
    /**
     * 查询与某好友之间未读消息数
     * GET /api/message/unread-count?friendId=2
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(@RequestParam Integer friendId) {
        return messageService.countUnread(currentUserId(), friendId);
    }
    /**
     * 查询总未读数（好友列表顶部红点）
     * GET /api/message/total-unread
     */
    @GetMapping("/total-unread")
    public Result<Integer> totalUnread() {
        return messageService.countTotalUnread(currentUserId());
    }
}
