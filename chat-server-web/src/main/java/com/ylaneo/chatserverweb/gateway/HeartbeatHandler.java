package com.ylaneo.chatserverweb.gateway;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;


public class HeartbeatHandler extends Thread {

    @Autowired
    private LegacyPushNotifier pushNotifier;//用字段注入而不是构造参数，因为这里要被 new Thread() 启动

    private final Socket socket;
    private final Integer userId;
    private boolean running = true;
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
                    //如果收到桌面端的ping消息
                    if ("ping".equals(msg)) {
                        System.out.println("[Heartbeat] 收到客户端心跳 userId=" + userId +"时间: "+ LocalDateTime.now());
                        os.write("pong".getBytes());
                        os.flush();
                    } else if ("close".equals(msg)) {
                        System.out.println("[Heartbeat] 收到客户端关闭 userId=" + userId+"时间: "+ LocalDateTime.now());
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
            //closeQuietly(socket);
            if(socket!=null){
                try{
                    //将socket长连接关闭。
                    socket.close();
                } catch (IOException e) {
                    System.out.println("桌面端socket长连接关闭失败");
                    throw new RuntimeException(e);
                }
            }
            // 关键：从注册表里移除并广播下线
            if (pushNotifier != null) {
                pushNotifier.handleDisconnect(userId);
            }
        }
    }

    //关闭socket连接的函数
    private void closeQuietly(Socket socket ) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
