package com.ylaneo.chatclientdesktop.session;

import com.ylaneo.chatclientdesktop.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {
    //创建一个静态用户信息实例
    private static UserSession userSession=new UserSession();//无参构造
    private User currentUser;

    //这个session包含的方法
    public static UserSession getInstance(){
        return userSession;
    }
    //获取当前登录的用户的实例
    public User getCurrentUser(){
        return currentUser;
    }
    //登陆的时候初始化User类型的实体类currentUser
    public void login(User user){
        this.currentUser=user;
    }

    //退出登录
    public void logout(){
        this.currentUser=null;
    }
    //管理员权限就直接在获取到的User实体类currentUser调用getrole方法获取吧，不在session中写了
}
