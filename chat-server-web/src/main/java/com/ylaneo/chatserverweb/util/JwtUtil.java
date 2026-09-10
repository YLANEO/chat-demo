package com.ylaneo.chatserverweb.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Date;
import java.util.Map;

public class JwtUtil {

    private static final String KEY = "hajimi";

    //接收业务数据,生成token并返回
    //还定义了接受的参数是什么类型的，在登录生成jwt令牌时，将这个哈希表数据加入到生成的token中（这个token就是一个string类型的字符串）
    public static String genToken(Map<String, Object> claims) {
        return JWT.create()
                .withClaim("claims", claims) // 1. 往票据里塞数据（比如用户ID、姓名）
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 12))//有效时间12小时
//                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 10))//有效时间10秒，用于验证
                .sign(Algorithm.HMAC256(KEY));// 3. 用“秘钥”盖章，生成防伪签名
    }

    //接收token,验证token,并返回业务数据
    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(KEY))
                .build()
                .verify(token)
                .getClaim("claims")
                .asMap();
    }

}