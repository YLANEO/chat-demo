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
    private boolean running = true;//
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
                    //这里会阻塞，直到有socket通道中有数据来。
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
                        System.out.println("[Heartbeat] 向客户端发送心跳响应信息 userId=" + userId +"时间: "+ LocalDateTime.now());
                        os.flush();
                    } else if ("close".equals(msg)) {
                        System.out.println("[Heartbeat] 收到客户端关闭 userId=" + userId+"时间: "+ LocalDateTime.now());
                        running = false;
                    }
                    // 这里忽略其他消息：聊天/消息推送都已经由LegacyPushNotifier处理

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
            System.out.println("[Heartbeat] 连接异常，直接退出循环 ");
        } finally {
            System.out.println("[Heartbeat] 结束该线程： userId=" + userId);
            //closeQuietly(socket);
            if(socket!=null){
                try{
                    //将socket长连接关闭。
                    socket.close();
                } catch (IOException e) {
                    System.out.println("[Heartbeat] 桌面端socket长连接关闭失败:userId=" + userId+ "\n err："+e.getMessage());
                    //throw new RuntimeException(e);//这里不要抛异常，抛了异常后面的handleDisconnect内容就执行不了了。
                }
            }
            // 关键：从注册表里移除并广播下线
            if (pushNotifier != null) {
                try{
                    pushNotifier.handleDisconnect(userId);
                } catch (Exception e) {
                    System.out.println("[Heartbeat] 下线广播失败 userId："+userId+"\n err："+e.getMessage());
                    //finally 抛异常会屏蔽掉外层 try 的异常
                    //throw new RuntimeException(e);
                }
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
