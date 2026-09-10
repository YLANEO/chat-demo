package com.ylaneo.chatserverweb.gateway;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * swing 客户端长连接的心跳监测线程。
 *
 * 来源（迁移说明）：
 *   从 chat-server/src/main/java/com/ylaneo/chatserver/handler/HeartbeatHandler.java
 *   完整搬过来，差异只有两处：
 *     1. 不再依赖 chat-server/socket/Server.java
 *     2. 改用 LegacyPushNotifier 提供的方法：
 *        - unregisterPush 代替 Server.removeUser
 *        - handleDisconnect 代替 Server.userDisconnected
 *        （handleDisconnect 内部会 unregisterPush + 调
 *         UserService.markUserOfflineAndNotify 让好友知道下线）
 *
 * 协议：
 *   - 客户端发 "ping" -> 服务端回 "pong"（接续连接）
 *   - 客户端发 "close" -> 服务端退出循环，主动关连接
 *   - 超时（35 秒无消息）-> 服务端主动发 "ping" 探测
 */
public class HeartbeatHandler extends Thread {

    @Autowired
    private LegacyPushNotifier pushNotifier;//用字段注入而不是构造参数，因为这里要被 new Thread() 启动

    private final Socket socket;
    private final Integer userId;
    private volatile boolean running = true;
    private InputStream is;
    private OutputStream os;

    public HeartbeatHandler(Socket socket, Integer userId, LegacyPushNotifier pushNotifier) {
        this.socket = socket;
        this.userId = userId;
        this.pushNotifier = pushNotifier;
    }

    @Override
    public void run() {
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            byte[] buffer = new byte[1024];

            while (running) {
                //35秒无消息超时——超时则主动发心跳探测
                socket.setSoTimeout(35000);

                try {
                    int len = is.read(buffer);

                    if (len <= 0) {
                        //连接断开
                        break;
                    }

                    String msg = new String(buffer, 0, len).trim();

                    if ("ping".equals(msg)) {
                        System.out.println("[Heartbeat] 收到客户端心跳 userId=" + userId);
                        os.write("pong".getBytes());
                        os.flush();
                    } else if ("close".equals(msg)) {
                        System.out.println("[Heartbeat] 收到客户端关闭 userId=" + userId);
                        running = false;
                    }
                    // 其他消息这里忽略：聊天/消息推送都已经由 LegacyPushNotifier 处理

                } catch (SocketTimeoutException e) {
                    // 超时主动探测
                    try {
                        os.write("ping".getBytes());
                        os.flush();
                        System.out.println("[Heartbeat] 主动探测 userId=" + userId);
                    } catch (IOException ex) {
                        // 探测失败说明连接已断
                        break;
                    }
                }
            }
        } catch (IOException e) {
            // 连接异常，直接退出循环
        } finally {
            System.out.println("[Heartbeat] 结束 userId=" + userId);
            closeQuietly(socket);
            // 关键：从注册表里移除并广播下线
            if (pushNotifier != null) {
                pushNotifier.handleDisconnect(userId);
            }
        }
    }

    private void closeQuietly(Socket s) {
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }
}
