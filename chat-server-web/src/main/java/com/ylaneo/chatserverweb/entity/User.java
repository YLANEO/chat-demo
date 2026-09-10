package com.ylaneo.chatserverweb.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer userid;
    private String username;
    private String password;
    private String nickname;
    private String avatar_url;
    private Integer status;
    // 登录成功时携带的 jwt token，web 端和 desktop 端都可能用到
    private String token;
}
