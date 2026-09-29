package com.ylaneo.chatserverweb.gateway;

import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.service.UserService;
import com.ylaneo.chatserverweb.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//需要将两端的统一到这里
//统一管理swing客户端的长连接socket，并提供"双向推送"能力。
@Component
public class LegacyPushNotifier {


    private final Map<Integer, Socket> legacyOnlineUsers = new ConcurrentHashMap<>();

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Autowired
    private UserService userService;

    //注册/注销swing长连接（长连接分支）

    //注册一个swing客户端的长连接socket。对应ServerThread.dispatch()里的Server.registerUser(userId,socket)
    public void registerPush(Integer userId, Socket socket) {
        legacyOnlineUsers.put(userId, socket);
        System.out.println("[Legacy] swing 长连接已注册: userId=" + userId);
    }

    //注销swing长连接。对应HeartbeatHandler finally块里的Server.removeUser(userId)。
    public void unregisterPush(Integer userId) {
        Socket removed = legacyOnlineUsers.remove(userId);
        if (removed != null) {
            System.out.println("[Legacy] swing 长连接已注销: userId=" + userId);
        }else {
            System.out.println("[Legacy] swing 长连接注销失败: userId=" + userId);
        }
    }

    //swing长连接断开操作：1.把用户状态置为离线2.通知好友该用户下线
    public void handleDisconnect(Integer userId) {
        unregisterPush(userId);
        // 这里直接复用 web 版 UserService 已有的方法
        userService.markUserOfflineAndNotify(userId);
    }

    // 推送：给单个用户
    public void pushTo(Integer userId, String legacyMessage) {
        pushTo(userId, legacyMessage, null);
    }

     //给单个用户推送消息，同时通知 web 端和 swing 端。
    public void pushTo(Integer userId, String legacyMessage, Object webJsonContent) {
        // 1) WebSocket 推送（web 端）
        if (webJsonContent != null) {
            try {
                webSocketHandler.pushToUser(userId, webJsonContent);
            } catch (Exception e) {
                System.out.println("[Legacy] web 端推送异常: " + e.getMessage());
            }
        }

        // 2) Swing 端：写 socket
        Socket s = legacyOnlineUsers.get(userId);
        if (s == null || s.isClosed()) {
            return;
        }
        try {
            synchronized (s) {
                s.getOutputStream().write(legacyMessage.getBytes());
                s.getOutputStream().flush();
            }
            System.out.println("[Legacy] swing 推送成功: userId=" + userId + ", msg=" + legacyMessage);
        } catch (IOException e) {
            System.out.println("[Legacy] swing 推送异常: " + e.getMessage());
            legacyOnlineUsers.remove(userId);
        }
    }

    // 推送：给多个用户（用于状态更新广播等场景）


    public void pushToUsers(List<Integer> userIds, String legacyMessage, Object webJsonContent) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        //WebSocket推送（web端）：一次性给所有人推同一个JSON
        if (webJsonContent != null) {
            try {
                webSocketHandler.pushToUsers(userIds, webJsonContent);
            } catch (Exception e) {
                System.out.println("[Legacy] web 端批量推送异常: " + e.getMessage());
            }
        }

        //Swing端：逐个写socket
        for (Integer uid : userIds) {
            Socket s = legacyOnlineUsers.get(uid);
            if (s == null || s.isClosed()) {
                continue;
            }
            try {
                synchronized (s) {
                    s.getOutputStream().write(legacyMessage.getBytes());
                    s.getOutputStream().flush();
                }
            } catch (IOException e) {
                System.out.println("[Legacy] swing 推送失败: userId=" + uid);
                legacyOnlineUsers.remove(uid);
            }
        }
    }


    public void pushToUsers(List<Integer> userIds, String legacyMessage) {
        pushToUsers(userIds, legacyMessage, null);
    }

    // 工具方法：构造web端的JSON

    /*用于构造 web 端的 statusUpdate 推送内容，与 swing 端的 <push><statusUpdate> 对应。
    因为 web 端的推送内容结构是 Map<String,Object>，为了复用 ChatWebSocketHandler.pushToUsers，
    这里工具方法集中一下，避免各 Service 各自造。*/
    public static Map<String, Object> buildWebStatusUpdate(Integer userId, Integer status) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "statusUpdate");
        msg.put("userId", userId);
        msg.put("status", status);
        return msg;
    }

    //构造web端的messageAdd推送内容。
    public static Map<String, Object> buildWebMessageAdd(Integer senderId, Integer messageId) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "messageAdd");
        msg.put("senderId", senderId);
        msg.put("messageId", messageId);
        return msg;
    }

    /**
     * 用于构造 web 端的 friendListUpdate 推送内容。
     */
    public static Map<String, Object> buildWebFriendListUpdate(Integer friendId) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "friendListUpdate");
        msg.put("friendId", friendId);
        return msg;
    }

    /**
     * 内部工具：把 entity.User 转成 web 端推送要的用户信息 Map。
     * 当前 web 端的推送逻辑（如 ContactServiceImpl.sendFriendRequest）使用
     * 一个 WebSocketMessage DTO；这里给出 Map 版本供 LegacyPushNotifier 直接用。
     */
    public static Map<String, Object> toUserMap(User u) {
        if (u == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("userid", u.getUserid());
        m.put("username", u.getUsername());
        m.put("nickname", u.getNickname());
        m.put("avatar_url", u.getAvatar_url());
        m.put("status", u.getStatus());
        return m;
    }
}
