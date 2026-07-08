package com.ylaneo.chatserver.socket;

import com.ylaneo.chatserver.handler.ServerThread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    public Server() throws IOException {
        // 打开一个公开的对外接口，并保持监听，客户端使用同一个端口号进行访问
        System.out.println("server打开一个公开的对外接口20001，并保持监听");
        ServerSocket serverSocket = new ServerSocket(20001);

        Boolean on_off_flag = true;
        while (on_off_flag) {//循环？还是阻塞？为什么这个值在后面没有被修改on_off_flag？？一直打开线程吗？不关吗？？
            //当有一个客户端来访问时，创建一个代表客户端的对象（socket）？？？
            Socket socket = serverSocket.accept();// 来访这个动作在哪里知道的？？通过这个方法accept？？

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
