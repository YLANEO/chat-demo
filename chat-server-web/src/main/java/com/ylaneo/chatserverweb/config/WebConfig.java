package com.ylaneo.chatserverweb.config;

//方便前端访问本地路径的修改

import com.ylaneo.chatserverweb.interceptors.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 注册拦截器
        // 在原来的排除路径基础上，新增了 "/uploads/**"，防止访问图片时报 401 错误
        registry.addInterceptor(loginInterceptor)
                //放行前端发送来的下面的路径的访问请求，其他路径的访问请求都要经过注入的loginInterceptors拦截器处理
                .excludePathPatterns("/user/register", "/user/login", "/uploads/**");
    }

    //注意这里有一个重写，就是把WebMvcConfigurer里面本来有的方法进行...了。定义了资源处理器的路径？然后配置了资源的访问路径？
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 2. 静态资源映射
        // 配置虚拟路径：当浏览器访问 http://localhost:8080/uploads/xxx 时
        // 实际上是去你桌面的物理文件夹 C:/Users/gin/Desktop/spring_flies/ 找图
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:D:/files_D/avatar_chat_demo");
    }

//    //跨域配置，暂时不用。还是用vue配置代理的方式
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("http://localhost:5173", "http://localhost:8080")
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//    }
}
