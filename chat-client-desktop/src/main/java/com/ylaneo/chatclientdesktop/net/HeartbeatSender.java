package com.ylaneo.chatclientdesktop.net;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;

//心跳机制线程，running为false时就结束该线程
public class HeartbeatSender extends Thread {
    private Socket socket;
    private volatile boolean running = true;

    public HeartbeatSender(Socket socket) {
        //
        this.socket = socket;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(30000);// 每30秒发送一次心跳
                //这里也有问题：这里采用的是轮询检测socket有没有断开连接，可能还没来得及执行这个if判断语句就关闭窗口了，可能导致这个线程还没有结束？吗？
                if (socket.isClosed()) {//在Mainview中关闭了socket连接之后，这个线程也就结束了
                    System.out.println("HeartbeatSender线程结束 ");
                    break;
                }

                socket.getOutputStream().write("ping".getBytes());//发送心跳，
                socket.getOutputStream().flush();
                System.out.println("向服务端发送心跳：ping "+"时间: "+ LocalDateTime.now());
            } catch (InterruptedException e) {
                break;
            } catch (IOException e) {
                running = false;// 连接异常，停止心跳
            }
        }
    }
}