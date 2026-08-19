package com.ylaneo.chatclientdesktop.net;

import com.ylaneo.chatclientdesktop.util.ResponseHandler;
import com.ylaneo.chatclientdesktop.view.MainView;
import com.ylaneo.chatclientdesktop.view.pages.ContactView;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.awt.*;
import java.util.List;

//获取服务端来的推送信息，进行信息更新
public class PushListener implements Runnable {
    private Socket socket;

    public PushListener(Socket socket) {
        //
        this.socket = socket;
    }

//    @Override
//    public void run() {
//        try {
//            InputStream is = socket.getInputStream();
//            byte[] buffer = new byte[65535];
//            while (true) {
//                int len = is.read(buffer);
//                if (len <= 0) break;
//                String msg = new String(buffer, 0, len);
//                System.out.println("收到推送: " + msg);
//                handlePush(msg);
//            }
//        } catch (IOException e) {
//            System.out.println("推送连接断开: " + e.getMessage());
//        }
//    }
    //启动线程
    @Override
    public void run() {
        //
        try {
            InputStream is = socket.getInputStream();//读取服务端发来的信息
            byte[] buffer = new byte[65535];
            while (true) {//这里在循环，因为是长连接，和服务端的serverThread（短连接，调用时运行完一次就结束线程）对比来看。和服务端的server中的while循环是一样的。
                int len = is.read(buffer);//如果没有数据就挂起线程，变成阻塞状态。有数据就读取服务端的推送数据到buffer中
                if (len <= 0) {
                    System.out.println("线程PushListener的while循环结束");
                    break;//socket连接关闭后（MainView中），这里就会触发，这个线程也就会结束了。
                }


                String msg = new String(buffer, 0, len).trim();
                System.out.println("收到服务端的推送消息："+msg);//记录到终端

                //收到服务端心跳ping，返回响应信息pong
                if ("ping".equals(msg)) {
                    //响应服务端的探测心跳
                    socket.getOutputStream().write("pong".getBytes());
                    socket.getOutputStream().flush();
                    System.out.println("收到服务端发送的心跳信息：ping。发送回响应信息：pong ");
                    continue;
                }

                handlePush(msg);//处理接受到的推送信息
            }
            System.out.println("线程PushListener结束");
        } catch (IOException e) {
            System.out.println("检测到推送socket连接断开，结束PushListener线程 : " + e.getMessage());
        }
    }
    //根据服务端的信息，更新好友在线状态
    private void handlePush(String msg) {
        if (!msg.startsWith("<push><statusUpdate>")) {
            return;
        }

        ResponseHandler responseHandler = new ResponseHandler();
        List<String> params = responseHandler.handleParams(msg);

        if (params.size() < 4) {
            return;
        }

        Integer userId;
        Integer status;

        try {
            userId = Integer.valueOf(params.get(2));
            status = Integer.valueOf(params.get(3));
        } catch (NumberFormatException e) {
            return;
        }

        Integer finalUserId = userId;
        Integer finalStatus = status;

        SwingUtilities.invokeLater(() -> refreshContactView(finalUserId, finalStatus));//更新：根据要修改的好友的id和status，
    }
    //更新mainview-contactview窗口中的显示信息
    private void refreshContactView(Integer userId, Integer status) {//更新好友在线状态
        Window[] windows = Window.getWindows();//获得当前的所有窗口
        //
        for (Window window : windows) {
            if (!(window instanceof MainView)) {//如果找到的窗口不是MainView的一个实例
                continue;//直接跳过这次循环之后的内容，进入下一次循环
            }
            if (!window.isDisplayable()) continue;//  关键这一行，不然会找到已经被dispose，但是还残留在内存里的窗口。

            MainView mainView = (MainView) window;//找到了主页面mainview

            for (Component component : mainView.contentPanel.getComponents()) {
                if (component instanceof ContactView) {
                    ContactView contactView = (ContactView) component;//找到主页面中的好友页面contactview
                    contactView.updateFriendStatus(userId, status);//更新好友页面：根据好友的id和status字段，更新该好友的在线状态
                    return;
                }
            }
        }
    }

    private Component getReferenceComponent() {
        //
        return null;// 占位，框架线程里不需要真的用这个
    }
}