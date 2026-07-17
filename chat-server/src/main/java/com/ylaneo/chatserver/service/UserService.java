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
    //修改在线状态
    String updateUserStatus(String request);
    //用户头像
    String updateUserAvatar(String request);
    //用户昵称
    String updateNickname(String request);
    //获取用户详细信息
    String getUserData(String request);
}
