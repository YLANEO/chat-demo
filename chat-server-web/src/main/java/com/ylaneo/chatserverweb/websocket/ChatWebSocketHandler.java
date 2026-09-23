package com.ylaneo.chatserverweb.websocket;

import com.ylaneo.chatserverweb.dto.WebSocketMessage;
import com.ylaneo.chatserverweb.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    // 懒取 UserService，打破与 UserServiceImpl 的构造期循环依赖
    private UserService getUserService() {
        return applicationContext.getBean(UserService.class);
    }

    /**
     * onlineUsers 的作用和 chat-server 项目中的 Server.onlineUsers 完全一样
     * key: userId（登录用户的 ID）
     * value: WebSocketSession（该用户的 WebSocket 连接）
     *
     * 为什么要用 Map 而不是 List？
     * 因为推送消息时需要"找到指定用户的连接"，O(1) 查找
     * ConcurrentHashMap 保证多线程安全，多个请求同时推送消息时不会出问题
     */
    private final Map<Integer, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 客户端连接建立成功时调用
     * 类似 Server.registerUser(userId, socket)
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从session中取出userId（登录时存入的，详见登录接口）
        Integer userId = getUserIdFromSession(session);
        if (userId != null) {
            onlineUsers.put(userId, session);
            // 通知好友：该用户上线
            getUserService().markUserOnlineAndNotify(userId);
            System.out.println("用户连接建立: userId=" + userId);
        }
    }

    /**
     * 收到客户端发来的文本消息时调用
     * 类似 ServerThread.run() 里的 request 处理逻辑
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);

        // 这里可以解析 payload 做路由分发（类似原来的 ServerThread.dispatch）
        // 简单起见，直接用 JSON 格式：
        // {"type": "chat", "senderId": 1, "receiverId": 2, "content": "你好"}
        try {
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);
            // 根据 type 分发到不同处理逻辑
            // routeMessage(wsMessage);
        } catch (Exception e) {
            System.out.println("消息解析失败: " + e.getMessage());
        }
    }

    /**
     * 连接关闭时调用
     * 类似 Server.userDisconnected(userId)
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Integer userId = getUserIdFromSession(session);
        if (userId != null) {
            onlineUsers.remove(userId);
            // 通知好友：该用户下线
            getUserService().markUserOfflineAndNotify(userId);
            System.out.println("用户断开连接: userId=" + userId);
        }
    }

    /**
     * 发生错误时调用
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.out.println("WebSocket 传输错误: " + exception.getMessage());
        Integer userId = getUserIdFromSession(session);
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    /**
     * ====================== 以下是给 Service 层调用的推送方法 ======================
     * 这些方法不是 WebSocket 协议的一部分，而是我们在 Handler 里额外提供的
     * Service 层调用这些方法，把消息推送给指定用户或广播
     */

    /**
     * 向指定用户推送消息
     * 等同于原来 chat-server 中的 Server.pushStatusUpdate(userId, message)
     */
    public void pushToUser(Integer targetUserId, Object message) {
        WebSocketSession session = onlineUsers.get(targetUserId);
        if (session == null || !session.isOpen()) {
            System.out.println("推送失败，用户不在线: userId=" + targetUserId);
            return;
        }
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            System.out.println("推送成功: targetUserId=" + targetUserId + ", message=" + jsonMessage);
        } catch (IOException e) {
            System.out.println("推送异常: " + e.getMessage());
            onlineUsers.remove(targetUserId);
        }
    }

    /**
     * 向所有在线用户广播消息
     */
    public void broadcastToAll(Object message) {
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }
        for (Map.Entry<Integer, WebSocketSession> entry : onlineUsers.entrySet()) {
            try {
                if (entry.getValue().isOpen()) {
                    entry.getValue().sendMessage(new TextMessage(jsonMessage));
                }
            } catch (IOException e) {
                onlineUsers.remove(entry.getKey());
            }
        }
    }

    /**
     * 向多个指定用户推送消息（用于状态变更广播给所有好友）
     */
    public void pushToUsers(List<Integer> targetUserIds, Object message) {
        String jsonMessage;
        try {
            jsonMessage = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }
        for (Integer userId : targetUserIds) {
            WebSocketSession session = onlineUsers.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    onlineUsers.remove(userId);
                }
            }
        }
    }

    // ====================== 辅助方法 ======================

    /**
     * 从WebSocketSession中取出userId
     * 登录时会在 session 中存入userId，格式如：
     * uri: ws://localhost:8080/ws/chat?userId=123
     */
    private Integer getUserIdFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();//"userId=123"
            if (query != null && query.startsWith("userId=")) {
                return Integer.valueOf(query.substring(7));
            }
        } catch (Exception e) {
            System.out.println("解析 userId 失败: " + e.getMessage());
        }
        return null;
    }

    // Getter，供外部查询在线状态
    public Map<Integer, WebSocketSession> getOnlineUsers() {
        return onlineUsers;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}