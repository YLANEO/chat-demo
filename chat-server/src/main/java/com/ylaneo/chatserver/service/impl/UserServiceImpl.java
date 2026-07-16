package com.ylaneo.chatserver.service.impl;

import com.ylaneo.chatserver.dao.UserDao;
import com.ylaneo.chatserver.dao.impl.UserDaoImpl;
import com.ylaneo.chatserver.dto.RegisterDto;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.service.UserService;
import com.ylaneo.chatserver.util.RequestHandler;

import java.util.List;

public class UserServiceImpl implements UserService {

    // ======================================== 感觉这两个拿出来复用好一点？？？ ========================================

    RequestHandler requestHandler=new RequestHandler();
    UserDao userDao=new UserDaoImpl();


    // ======================================== 接口实现类具体内容 ========================================
    //login
    @Override
    public String login(String request) {
        //调用工具类中的方法解析出每个参数，将每个参数按照顺序放入params列表中
        List<String>params=requestHandler.handleParams(request);
        String username=params.get(1);//注意这里不是0，而是1，因为0位置是操作类型
        String password=params.get(2);
        Result<User> r1=userDao.login(username,password);
        User user=r1.getData();//使用一个对象接受返回的数据
        //int id=user.getUserid();
        //int role=user.getRole();
        //在组装reseponse之前先判断操作是否成功，不然data里的user对象没有数据，使用get方法会报错
        if(r1.getCode()==0){
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">"+"<"+user.getUserid()+">"+"<"+user.getUsername()+">";//按照顺序放入响应代码，响应信息，用户id，用户名，用户角色代码
            return response;
        }else{
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">";
            return response;
        }
    }

    //register
    @Override
    public String register(String request) {
        //1.解析出各个参数
        List<String>params=requestHandler.handleParams(request);
        String username=params.get(1);
        String password=params.get(2);
        String repassword=params.get(3);

        //2.调用dao的接口获得result对象数据
        RegisterDto registerDto=new RegisterDto();
        registerDto.setUsername(username);
        registerDto.setPassword(password);
        registerDto.setRePassword(repassword);
        Result<User> r1=userDao.register(registerDto);
        //3.根据不同的响应码组装不同的response返回信息
        if(r1.getCode()==0){
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">";//按照顺序放入响应代码，响应信息，用户id，用户名，用户角色代码
            return response;
        }else{
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">";
            return response;
        }
    }

    @Override
    public String updateUsername(String request) {
        //1.解析出各个参数
        List<String>params=requestHandler.handleParams(request);
        Integer userId=Integer.valueOf(params.get(1)) ;//
        String reUsername=params.get(2);

        //2.调用dao的接口获得result对象数据
        Result<User> r1=userDao.updateUsername(userId,reUsername);
        //3.根据不同的响应码组装不同的response返回信息
        if(r1.getCode()==0){
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">";//按照顺序放入响应代码，响应信息，用户id，用户名，用户角色代码
            return response;
        }else{
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">";
            return response;
        }
    }

    @Override
    public String getUsername(String request) {
        //调用工具类中的方法解析出每个参数，将每个参数按照顺序放入params列表中
        List<String>params=requestHandler.handleParams(request);
        Integer userId=Integer.valueOf(params.get(1)) ;//注意这里不是0，而是1，因为0位置是操作类型
        Result<User> r1=userDao.getUsername(userId);
        User user=r1.getData();//使用一个对象接受返回的数据
        //int id=user.getUserid();
        //int role=user.getRole();
        //在组装reseponse之前先判断操作是否成功，不然data里的user对象没有数据，使用get方法会报错
        if(r1.getCode()==0){
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">"+"<"+user.getUserid()+">"+"<"+user.getUsername()+">";//按照顺序放入响应代码，响应信息，用户id，用户名，用户角色代码
            return response;
        }else{
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">";
            return response;
        }
    }

    @Override
    public String updateUserStatus(String request) {
        List<String>params=requestHandler.handleParams(request);
        Integer userId=Integer.valueOf(params.get(1));
        Integer userStatus=Integer.valueOf(params.get(2));
        Result r=userDao.updateUserStatus(userId,userStatus);
        String response="<"+r.getCode()+">"+"<"+r.getMessage()+">";
        return response;
    }


}
