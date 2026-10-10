package com.ylaneo.chatclientdesktop.net;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

/* 如果要方便进行socket的长连接管理，可以再新建一个clientConnection类专门管理socke管理，但是暂时用不到，之后要用再添加 */
//用来进行socket的建立和断开，并发送不同的请求，各种请求都应该在这里进行发送
public class RequestSender {
    // ======================================== 定义要用的类，并初始化 ========================================
    Socket socket;
    InputStream is;//方法read用来接收数据
    OutputStream os;//方法write用来发送数据
    //空构造
    public  RequestSender(){

    }

    // ======================================== 写一个函数专门进行请求的发送和接受server的回传数据，复用 ========================================

    public String sendRequest(String request) throws IOException {//向上抛，但是遇到没解决的异常client程序就会卡住
        //这里socket的创建交给下面的sendrequest方法完成，因为
        try{
            System.out.println("开始尝试与server建立socket连接");
            socket=new Socket("127.0.0.1",20001);//与server建立连接，这里就是客户端向服务端敲门的动作
            is=socket.getInputStream();
            os=socket.getOutputStream();
            System.out.println("成功与server建立socket连接");
        }catch (UnknownHostException e) {//这里为什么要抛出两种异常？？
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //write方法发送
        os.write(request.getBytes());//通过write方法向server发送请求
        System.out.println("向server发送了请求"+request);

        byte[] buffer = new byte[65535];//为什么使用byte类？

        //read方法接收
        int len=is.read(buffer);//将从server流出来的数据用容器变量buffer来接收
        System.out.println("接收到server发来的长度为: "+ len +"的数据: "+new String(buffer,0,len) );//这么写才是数据对应的字符串数据，不然字符串会自动用空字符填写

        //关闭socket连接，因此：这里面的socket连接是短连接
        socket.close();
        System.out.println("关闭socket短连接 时间： "+ LocalDateTime.now());

        //返回收到的数据
        //return new String(buffer);//转换成String之后返回
        return new String(buffer,0,len);//转换成String之后返回
    }

    // ======================================== 启动 ========================================
//    public static void main(String[] args) throws IOException {
//        RequestSender requestSender=new RequestSender();
//        //测试发送请求，
//        String result=requestSender.login("usera","111");
//        System.out.println("返回的结果："+result);
//    }
}
