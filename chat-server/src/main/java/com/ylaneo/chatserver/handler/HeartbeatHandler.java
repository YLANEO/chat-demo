package com.ylaneo.chatserver.handler;

import com.ylaneo.chatserver.socket.Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

//
public class HeartbeatHandler extends Thread {
    private Socket socket;
    private Integer userId;
    private volatile boolean running = true;
    private InputStream is;
    private OutputStream os;

    public HeartbeatHandler(Socket socket, Integer userId) {
        this.socket = socket;
        this.userId = userId;
    }

    @Override
    public void run() {
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            byte[] buffer = new byte[1024];

            while (running) {
                //读取消息，设置超时防止永久阻塞
                socket.setSoTimeout(35000); //35秒超时

                try {
                    int len = is.read(buffer);

                    if (len <= 0) {
                        break;  // 连接断开
                    }

                    String msg = new String(buffer, 0, len).trim();

                    //心跳消息处理
                    if ("ping".equals(msg)) {
                        System.out.println("收到客户端用户id: "+userId+" 的心跳消息：ping ");
                        os.write("pong".getBytes());
                        os.flush();
                    } else if ("close".equals(msg)) {
                        //客户端主动关闭
                        System.out.println("收到客户端用户id: "+userId+" 的关闭连接消息：close");
                        running = false;
                    }

                } catch (SocketTimeoutException e) {
                    //超时就发送心跳探测给客户端
                    os.write("ping".getBytes());
                    os.flush();
                    System.out.println("未收到客户端用户id: "+userId+" 的心跳消息，超时");
                }
            }
        } catch (IOException e) {
            // 连接异常
        } finally {
            System.out.println("HeartbeatHandler线程 finally 客户端用户id： "+userId );
            Server.removeUser(userId);
            closeQuietly(socket);
            Server.userDisconnected(userId);//下线
        }
    }
    //关闭该socket连接
    private void closeQuietly(Socket s) {
        //
        try {
            if (s != null) s.close();
        } catch (IOException ignored) {}
    }
}