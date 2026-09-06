package com.ylaneo.chatserverweb.interceptors;

import com.ylaneo.chatserverweb.util.JwtUtil;
import com.ylaneo.chatserverweb.util.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/* 请求拦截器，在前端发送来的请求到到controller层之前进行统一的拦截和处理，同时使用了jwt的解析函数解析发送的请求携带的token，如果解析失败，直接返回请求失败，不用到controller层处理
如果解析成功了，那么
用ThreadLocal存储解析出来的用户信息（username和 userid）
所以说：其实所有的前端请求（除了注册和登录以外）都需要进行ThreadLocal的存储处理
*/

@Component//注入到IOC容器中，还要实现HandlerInterceptor父类，并重写prehandle方法
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        /*前端发任何带自定义header（Authorization）的跨域请求，浏览器会先发一个OPTIONS预检请求。预检请求没有Authorization头，
        被拦截器一拦直接返回401会导致整个跨域请求全部失败。*/
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

/*      获得令牌的名称"Authorization"是在哪里指定的？？答：大概是前端指定的，从前端获得就行了，
        前端会包含HttpServletRequest request, HttpServletResponse response这些东西，

*/
        //获得令牌
        String token=request.getHeader("Authorization");
        //进行判断
        try
        {
            //解析令牌
            Map<String,Object> claims= JwtUtil.parseToken(token);
            //将解析出来的数据（或者是一个对象）利用ThreadLocal进行存储
            ThreadLocalUtil.set(claims);
            //成功解析,放行
            return true;
        }
        //未能成功解析时
        catch (Exception e)
        {
            /*//
            response.setStatus(401);//设置状态码，刚好这个方法的参数中就有这个类型的对象HttpServletResponse response
            return false;//拦截*/
            //
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":1,\"message\":\"token无效或已过期\",\"data\":null}");
            return false;

        }
    }

    //自动调用，类似于前端的钩子函数
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清空ThreadLocal中的数据防止内存溢出
        //无论prehandle函数是否处理成功，这个函数都会调用，清除ThreadLocal中的数据
        ThreadLocalUtil.remove();
    }
}
