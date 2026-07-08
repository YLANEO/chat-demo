package com.ylaneo.chatserver.service;


//与server中的userdao中的接口进行对接
public interface UserService {
    //login
    String login(String request);
    //register
    String register(String request);
    //更新用户名
    String updateUsername(String request);
    //根据id获取用户名（用户信息）
    String getUsername(String request);
}
