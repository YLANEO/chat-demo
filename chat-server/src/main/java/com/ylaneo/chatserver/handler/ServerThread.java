package com.ylaneo.chatserver.handler;

import com.ylaneo.chatserver.service.ContactService;
import com.ylaneo.chatserver.service.MessageService;
import com.ylaneo.chatserver.service.UserService;
import com.ylaneo.chatserver.service.impl.ContactServiceImpl;
import com.ylaneo.chatserver.service.impl.MessageServiceImpl;
import com.ylaneo.chatserver.service.impl.UserServiceImpl;
import com.ylaneo.chatserver.socket.Server;
import com.ylaneo.chatserver.util.RequestHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

//server只需要与client建立socket连接然后把socket交给这个类，这个类中才进行数据的接受和发送，数据库操作，
public class ServerThread extends Thread {
    // ======================================== 定义要用的变量 ========================================
    Socket socket;
    InputStream is;
    OutputStream os;
    RequestHandler requestHandler=new RequestHandler();//用于解析数据流的类
    public ServerThread(Socket s) {
        //构造函数，接受server传的socket对象，为这个类里面的socket对象赋值（与client连接的socket对象初始化）
        socket = s;
    }

    // ======================================== 测试代码,只做参考，具体内容见下一个板块 ========================================
    /*public void doSomeThing () {//这个方法中的就是server要提供给客户端的服务相关的代码吗？

        try {
//            byte[] buffer = new byte[65535];//为什么要用byte类进行一次转换，从InputStream转换成byte？？？
            byte[] buffer = new byte[65535];
            *//* 解释：InputStream 就像一个水龙头，它一点一点地往外滴数据（字节）。但数据不能悬在空中，你需要一个桶来接住它，这个桶就是 byte[] buffer。*//*

            //这个socket对象是从Server类中传进来的，这个socket对象与客户端的socket对象是同一个对象？不是，但是连接的是同一个客户端socket对象
            is = socket.getInputStream();//
            int len = is.read(buffer);//读取客户端发送的消息，使用read方法,并记下长度
            System.out.println("收到一条长度为" + len + "的信息：" + new String(buffer,0,len));

            os = socket.getOutputStream();//发送信息回客户端，使用write方法
            os.write("what's up".getBytes());//将字符串转换为字节数组，然后发送给客户端
            System.out.println("已经发送了信息："+"what's up");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    // ======================================== 数据库操作代码 & socket读取和发送代码 ========================================

    //使用if-else根据请求的开始部分决定调用service层中的哪个接口
    public void dispatch() throws IOException {
        byte[]buffer=new byte[65535];
        //接受client的数据
        is=socket.getInputStream();
        int len=is.read(buffer);
        //解析前的预处理
        String request=new String(buffer,0,len);//获取实际的数据
        System.out.println("收到一条长度为" + len + "的信息：" + new String(buffer,0,len));//打印接收到的数据
        String response="";//要返回给client的内容
        //解析client的数据并调用dao的接口
        // ======================================== 使用if-else根据请求的开始部分决定调用哪个接口 ========================================


        //只留下if-else判断调用什么接口的内容，以及接受传回的String类型的response信息就行了
        if( request.startsWith("<login>") ){//检查收到的数据的前后缀确认请求类型
            UserService userService=new UserServiceImpl();
            response=userService.login(request);
        } else if (request.startsWith("<register>")) {
            UserService userService=new UserServiceImpl();
            response=userService.register(request);
        } else if (request.startsWith("<updateUsername>")) {
            UserService userService=new UserServiceImpl();
            response=userService.updateUsername(request);
        } else if (request.startsWith("<getUsername>")) {
            UserService userService=new UserServiceImpl();
            response=userService.getUsername(request);
        } else if (request.startsWith("<updateUserStatus>")) {
            UserService userService=new UserServiceImpl();
            response=userService.updateUserStatus(request);
        } else if (request.startsWith("<updateUserAvatar>")) {
            UserService userService=new UserServiceImpl();
            response=userService.updateUserAvatar(request);
        } else if (request.startsWith("<updateNickname>")) {
            UserService userService=new UserServiceImpl();
            response=userService.updateNickname(request);
        } else if (request.startsWith("<getUserData>")) {
            UserService userService=new UserServiceImpl();
            response=userService.getUserData(request);
        } else if (request.startsWith("<getFriendList>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.getFriendList(request);
        } else if (request.startsWith("<searchUserByKeyword>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.searchUserByKeyword(request);
        } else if (request.startsWith("<sendFriendRequest>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.sendFriendRequest(request);
        } else if (request.startsWith("<getFriendRequests>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.getFriendRequests(request);
        } else if (request.startsWith("<handleRequest>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.handleRequest(request);
        } else if (request.startsWith("<isFriend>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.isFriend(request);
        } else if (request.startsWith("<hasPendingRequest>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.hasPendingRequest(request);
        } else if (request.startsWith("<getFriendIds>")) {
            ContactService contactService=new ContactServiceImpl();
            response=contactService.getFriendIds(request);
        } else if (request.startsWith("<getAllMessage>")) {
            MessageService messageService=new MessageServiceImpl();
            response=messageService.getAllMessage(request);
        } else if (request.startsWith("<sendMessage>")) {
            MessageService messageService=new MessageServiceImpl();
            response=messageService.sendMessage(request);
        } else if (request.startsWith("<findMessageById>")) {
            MessageService messageService=new MessageServiceImpl();
            response=messageService.findMessageById(request);
        } else if (request.startsWith("<registerPushSocket>")) {//
            //对于 <registerPushSocket> 请求，特殊处理：保持连接不关闭
            List<String> params = requestHandler.handleParams(request);
            Integer userId = Integer.valueOf(params.get(1));
            Server.registerUser(userId, socket);
            //建立长连接的同时，启动独立线程处理该连接的心跳。注意：每个在线用户都需要一个这个线程，可能有点浪费服务端资源？
            new HeartbeatHandler(socket, userId).start();

            return;  // 不发送响应，不关闭连接，线程自己管理
        } else {//其他情况
//            response="<1><非法请求>";
            response = "<1>" + "<未知命令: " + request.substring(0, Math.min(20, request.length())) + ">";
        }
        //发送数据回client
        os=socket.getOutputStream();
        os.write(response.getBytes());//必须要调用getBytes方法进行数据流的回传
    }


    /**
     * 独立线程，开始干活
     */
    @Override
    public void run() {

        //doSomeThing();
        try {
            dispatch();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


}
