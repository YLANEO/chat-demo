package com.ylaneo.chatserver.dao;


import com.ylaneo.chatserver.dto.RegisterDto;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;

public interface UserDao {

    //注册接口
//    Result register(String username, String password, String rePassword , Integer role);
    Result register(RegisterDto registerDto);
    //登录接口:注意需要区分登录的角色（使用user中的role属性区分）
    Result login(String username,String password);
    //修改用户名
    Result updateUsername(Integer userId,String reUsername);
    //获得当前用户的username
    Result getUsername(Integer userId);
}
