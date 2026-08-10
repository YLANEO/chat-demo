package com.ylaneo.chatserver.socket;

import com.ylaneo.chatserver.handler.ServerThread;
import com.ylaneo.chatserver.service.impl.UserServiceImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    //key:userId,value:该用户的socket连接。通过用户id找到对应的socket连接
    public static Map<Integer, Socket> onlineUsers = new ConcurrentHashMap<>();

    // 用户登录时注册连接
    public static void registerUser(Integer userId, Socket socket) {
        onlineUsers.put(userId, socket);
    }

    //用户断开时移除
    public static void removeUser(Integer userId) {
        //
        onlineUsers.remove(userId);
    }

    // 发送推送状态更新信息给指定用户
    /*这只是当前简单文本协议下的临时修复。TCP 粘包问题仍然存在，但对于当前功能的第一轮验证，可以先这样做。等功能确认后，再升级成带消息分隔符或长度头的协议。*/
    public static void pushStatusUpdate(Integer targetUserId, String message) {
        Socket socket = onlineUsers.get(targetUserId);//根据好友的id（需要被推送当前用户的状态更新信息的用户），找出哈希表中的socket连接。当然也可能找不到（因为不在线）

        if (socket == null || socket.isClosed()) {//该好友不在线，就直接return
            return;
        }

        try {
            //避免多个线程同时写同一个 socket
            synchronized (socket) {
                socket.getOutputStream().write(message.getBytes());//
                socket.getOutputStream().flush();
            }

            System.out.println("已推送状态给好友: targetUserId=" + targetUserId + ", message=" + message);
        } catch (IOException e) {
            removeUser(targetUserId);
        }
    }

    //
    public static void userDisconnected(Integer userId) {
        Socket removedSocket = onlineUsers.remove(userId);

        if (removedSocket == null) {
            return;
        }

        UserServiceImpl userService = new UserServiceImpl();
        userService.markUserOfflineAndNotify(userId);//
    }

    public Server() throws IOException {
        // 打开一个公开的对外接口，并保持监听，客户端使用同一个端口号进行访问
        System.out.println("server打开一个公开的对外接口20001，并保持监听");
        ServerSocket serverSocket = new ServerSocket(20001);

        Boolean on_off_flag = true;
        while (on_off_flag) {//循环？还是阻塞？为什么这个值在后面没有被修改on_off_flag？？一直打开线程吗？不关吗？？
            //当有一个客户端来访问时，创建一个代表客户端的对象（socket）？？？
            /* 来访这个动作在哪里知道的？？通过这个方法accept？？
            答：accept() 是阻塞方法。调用这一行时，服务端线程会"停下来等"，什么都不干，直到有客户端发来TCP连接请求才返回。
            补充：触发点不是"客户端的outputstream调用了write方法"，而是"客户端主动来敲门（connect）"。
            敲门的动作就是client中RequestSender类中的这一句：socket=new Socket("127.0.0.1",20001);//与server建立连接，这里就是客户端向服务端敲门的动作 */
            Socket socket = serverSocket.accept();
            // 服务完成后断开
            ServerThread st = new ServerThread(socket);//创建这个包里的线程类的对象，把这里创建的与客户端连接的socket对象传给这个线程类用于构建这个线程类
			//st.run();
            st.start();//启动线程,不能用run方法，不然就不是以线程的方式启动的了
        }
        System.out.println("服务器关闭");//？？？？这里没有结束线程的代码啊，难道是在线程类中结束的？？自己控制自己，放权说是，吗？？？
    }

    public static void main(String[] args) {
        try {
            new Server();//启动这个类，进行监听
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
