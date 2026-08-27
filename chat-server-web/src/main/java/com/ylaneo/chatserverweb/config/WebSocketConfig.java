package com.ylaneo.chatserverweb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.ylaneo.chatserverweb.websocket.ChatWebSocketHandler;

@Configuration
@EnableWebSocket  // 启用 WebSocket 支持
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public WebSocketConfig(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 将 /ws/chat 路径映射到我们的 Handler
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .setAllowedOrigins("*");  // 允许所有来源，生产环境建议指定具体域名
    }
}