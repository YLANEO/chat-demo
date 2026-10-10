package com.ylaneo.chatclientdesktop.session;

import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.net.HeartbeatSender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.Socket;
import java.security.PrivateKey;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    //创建一个静态用户信息实例
    private static UserSession userSession=new UserSession();//无参构造
    private User currentUser;
    private Socket pushSocket;//登陆成功后创建的socket的引用，方便在这里管理



    private HeartbeatSender heartbeatSender;//当成一个单例来用，这样才能让用户调用到同一个线程，不然就需要new一个，那就是新的线程不是该用户的那个线程了

    //这个session包含的方法
    public static UserSession getInstance(){
        //
        return userSession;
    }
    //获取当前登录的用户的实例
    public User getCurrentUser(){
        //
        return currentUser;
    }
    //登陆的时候初始化User类型的实体类currentUser
    public void login(User user){
        //
        this.currentUser=user;
    }

    //退出登录
    public void logout(){
        //
        this.currentUser=null;
    }
    //socket引用绑定
    public void setPushSocket(Socket s){
        this.pushSocket=s;
    }
    //返回这个socket引用，方便进行关闭
    public Socket getPushSocket() {
        //
        return pushSocket;
    }
    //
    public HeartbeatSender getHeartbeatSender() {
        return heartbeatSender;
    }

    public void setHeartbeatSender(HeartbeatSender heartbeatSender) {
        this.heartbeatSender = heartbeatSender;
    }
}
