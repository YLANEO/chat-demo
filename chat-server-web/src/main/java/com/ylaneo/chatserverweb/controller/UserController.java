package com.ylaneo.chatserverweb.controller;

import com.ylaneo.chatserverweb.dto.RegisterDto;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.service.UserService;
import com.ylaneo.chatserverweb.util.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
//@CrossOrigin
public class UserController {
    @Autowired
    UserService userService;

    //登录
    @PostMapping("/login")
    public Result<String> login(@RequestParam String username, @RequestParam String password) {
        // web 端只需要 token，所以从 User 中把 token 取出来再返回
        Result<User> result = userService.login(username, password);
        if (result.getCode() == 0 && result.getData() != null) {
            return Result.success(result.getData().getToken());
        }
        return Result.<String>error(result.getMessage());
    }

    //注册
    @PostMapping("/register")
    public Result Register(@RequestBody RegisterDto registerDto) {
        return userService.register(registerDto);
    }

    //获取用户信息（不用参数，因为前端请求会带上token，在后端都要经过ThreadLocal步骤，获得用户数据）
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        //从ThreadLocal取出当前登录用户的ID
        Map<String,Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("userid");//注意这里的key要与userservice登录接口中设置的一致
        //查询用户信息
        return userService.getUserData(userId);
    }
}
