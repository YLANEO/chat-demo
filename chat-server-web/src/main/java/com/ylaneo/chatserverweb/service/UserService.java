package com.ylaneo.chatserverweb.service;


import com.ylaneo.chatserverweb.dto.RegisterDto;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;

public interface UserService {

    //注册接口
//    Result register(String username, String password, String rePassword , Integer role);
    Result register(RegisterDto registerDto);
    //登录接口:注意需要区分登录的角色（使用user中的role属性区分）
    Result login(String username,String password);
    //修改用户名
    Result updateUsername(Integer userId,String reUsername);
    //获得当前用户的username
    Result getUsername(Integer userId);
    //修改在线状态
    Result updateUserStatus(Integer userId,Integer userStatus);
    //修改用户头像
    Result updateUserAvatar(Integer userId,String userAvatarUrl);
    //更新昵称
    Result updateNickname(Integer userId, String newNickname);
    //获取用户信息
    Result<User> getUserData(Integer userId);
}
