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

/**
 * 统一管理 swing 客户端的长连接 socket，并提供"双向推送"能力。
 *
 * 背景：
 *   原来的 chat-server 用 Server.onlineUsers (Map<Integer,Socket>)保存swing长连接，
 *   并通过 Server.pushStatusUpdate(userId, message) 写字节流。
 *   现在合并进 chat-server-web 之后，两类客户端会同时存在：
 *     - Web 客户端：通过 ChatWebSocketHandler 推送（JSON）
 *     - Swing 客户端：通过这里的 legacyOnlineUsers 推送（尖括号字符串）
 *
 *   这个类的核心职责：
 *     1. 维护 legacyOnlineUsers (swing 长连接注册表)
 *     2. 提供 pushTo / pushToUsers 方法，同时通知 Web 端和 Swing 端
 *     3. 提供 registerPush / unregisterPush，配合 HeartbeatHandler 处理用户上下线
 *
 * 依赖关系：
 *   - 被 LegacyClientHandler 调用 (registerPush)
 *   - 被 HeartbeatHandler 调用 (unregisterPush)
 *   - 被各 ServiceImpl 调用 (pushToUsers / pushTo)，用于状态/好友/消息推送时双向通知
 */
@Component
public class LegacyPushNotifier {

    /**
     * swing 长连接注册表。
     * key: userId
     * value: 与该 swing 客户端保持长连接的 Socket
     *
     * 对应原代码：
     *   chat-server/socket/Server.java 的 public static Map<Integer,Socket>onlineUsers
     */
    private final Map<Integer, Socket> legacyOnlineUsers = new ConcurrentHashMap<>();

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    @Autowired
    private UserService userService;

    // 注册/注销swing长连接（长连接分支）

    /**
     * 注册一个swing客户端的长连接socket。
     * 对应 ServerThread.dispatch()里的Server.registerUser(userId, socket)
     */
    public void registerPush(Integer userId, Socket socket) {
        legacyOnlineUsers.put(userId, socket);
        System.out.println("[Legacy] swing 长连接已注册: userId=" + userId);
    }

    /**
     * 注销 swing 长连接。
     * 对应 HeartbeatHandler finally 块里的 Server.removeUser(userId)。
     */
    public void unregisterPush(Integer userId) {
        Socket removed = legacyOnlineUsers.remove(userId);
        if (removed != null) {
            System.out.println("[Legacy] swing 长连接已注销: userId=" + userId);
        }
    }

    /**
     * swing长连接断开时的副作用：
     *   1. 把用户状态置为离线
     *   2. 通知好友该用户下线
     * 对应 Server.userDisconnected -> markUserOfflineAndNotify。
     */
    public void handleDisconnect(Integer userId) {
        unregisterPush(userId);
        // 这里直接复用 web 版 UserService 已有的方法
        userService.markUserOfflineAndNotify(userId);
    }

    // ============================================================
    // 推送：给单个用户
    // ============================================================

    /**
     * 给单个用户推送消息，同时尝试 web 端和 swing 端。
     *
     * @param userId       目标用户 ID
     * @param legacyMessage 已经是尖括号格式的消息字符串，例如
     *                      "<push><statusUpdate><1><0>"
     *                      或者 "<push><messageAdd><2><123>"
     *                      同一个字符串会发给 swing；对 web 端同时推送一个最小 JSON 镜像。
     *
     * 这里只关心"推 swing"；web 端的推送调用方一般会另外做（保持现有 WebSocket 推送不变）。
     * 但为了方便，方法签名同时接受 legacy 字符串和可选的 webJsonContent。
     */
    public void pushTo(Integer userId, String legacyMessage) {
        pushTo(userId, legacyMessage, null);
    }

    /**
     * 给单个用户推送消息，同时通知 web 端和 swing 端。
     *
     * @param userId         目标用户 ID
     * @param legacyMessage  swing 端的尖括号字符串
     * @param webJsonContent 可选：发往 web 端的内容（任意对象，会被 ChatWebSocketHandler 序列化为 JSON）
     */
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

    /**
     * 给一组用户推送同一条swing消息。
     * webJsonContent是可选的：若提供，则同时给每个user推web端消息（避免重复序列化）。
     */
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

    /**
     * 仅给swing端批量推送（兼容原本只推 swing 的场景）。
     */
    public void pushToUsers(List<Integer> userIds, String legacyMessage) {
        pushToUsers(userIds, legacyMessage, null);
    }

    // 工具方法：构造web端的JSON

    /**
     * 用于构造 web 端的 statusUpdate 推送内容，与 swing 端的 <push><statusUpdate> 对应。
     * 因为 web 端的推送内容结构是 Map<String,Object>，为了复用 ChatWebSocketHandler.pushToUsers，
     * 这里工具方法集中一下，避免各 Service 各自造。
     */
    public static Map<String, Object> buildWebStatusUpdate(Integer userId, Integer status) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "statusUpdate");
        msg.put("userId", userId);
        msg.put("status", status);
        return msg;
    }

    /**
     * 用于构造 web 端的 messageAdd 推送内容。
     */
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
