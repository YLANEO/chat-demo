package com.ylaneo.chatclientdesktop.facade;


import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;

//与server中的userdao中的接口进行对接
public interface UserFacade {
    //login
    Result<User> login(String username, String password);
    //register
    Result register(String username, String password, String repassword);
    //更新用户名，updateUsername
    Result updateUsername(Integer userId,String reUsername);
    //根据id获得用户名，用于更新用户名接口
    Result<User> getUsername(Integer userId);
    //
    Result updateUserStatus(Integer userId,Integer userStatus);
    //上传用户头像url
    Result updateUserAvatar(Integer userId,String userAvatarUrl);
    //更新昵称
    Result updateNickname(Integer userId, String newNickname);
    //获取用户信息
    Result<User> getUserData(Integer userId);
}
