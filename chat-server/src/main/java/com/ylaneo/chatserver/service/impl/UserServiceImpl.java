package com.ylaneo.chatserver.service.impl;

import com.ylaneo.chatserver.dao.ContactDao;
import com.ylaneo.chatserver.dao.UserDao;
import com.ylaneo.chatserver.dao.impl.ContactDaoImpl;
import com.ylaneo.chatserver.dao.impl.UserDaoImpl;
import com.ylaneo.chatserver.dto.RegisterDto;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.service.UserService;
import com.ylaneo.chatserver.socket.Server;
import com.ylaneo.chatserver.util.RequestHandler;

import java.util.List;

public class UserServiceImpl implements UserService {

    // ======================================== 感觉这两个拿出来复用好一点？？？ ========================================

    RequestHandler requestHandler=new RequestHandler();
    UserDao userDao=new UserDaoImpl();
    ContactDao contactDao=new ContactDaoImpl();


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
        //添加上一个广播的步骤
        if (r.getCode() == 0) {
            broadcastStatusUpdate(userId, userStatus);//关键的触发动作：当前用户的在线状态发生变化  这个动作。
        }
        //
        String response="<"+r.getCode()+">"+"<"+r.getMessage()+">";
        return response;
    }

    @Override
    public String updateUserAvatar(String request) {
        List<String>params=requestHandler.handleParams(request);
        Integer userId=Integer.valueOf(params.get(1));
        String userAvatarUrl=params.get(2);
        Result r=userDao.updateUserAvatar(userId,userAvatarUrl);
        String response="<"+r.getCode()+">"+"<"+r.getMessage()+">";
        return response;
    }

    @Override
    public String updateNickname(String request) {
        List<String>params=requestHandler.handleParams(request);
        Integer userId=Integer.valueOf(params.get(1));
        String userNickname=params.get(2);
        Result r=userDao.updateNickname(userId,userNickname);
        String response="<"+r.getCode()+">"+"<"+r.getMessage()+">";
        return response;
    }

    @Override
    public String getUserData(String request) {
        List<String>params=requestHandler.handleParams(request);
        Integer userId=Integer.valueOf(params.get(1));
        Result<User> r1=userDao.getUserData(userId);
        User user=r1.getData();
        if(r1.getCode()==0){
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">"+"<"+user.getUsername()+">"+"<"+user.getNickname()+">"+"<"+user.getAvatar_url()+">"+"<"+user.getStatus()+">";//按照顺序放入响应代码，响应信息，用户id，用户名，用户角色代码
            return response;
        }else{
            String response="<"+r1.getCode()+">"+"<"+r1.getMessage()+">";
            return response;
        }
    }

    //当前用户在线状态发生变化时，拼接当前用户的id和变化后的status字段，并将该拼接信息推送给该用户的所有好友的客户端
    private void broadcastStatusUpdate(Integer userId, Integer status) {
        Result<List<Integer>> result = contactDao.getFriendIds(userId);//获取当前用户的所有好友的id

        if (result.getCode() != 0 || result.getData() == null) {
            return;
        }

        String pushMessage = "<push><statusUpdate><" + userId + "><" + status + ">";

        //广播给当前用户的所有好友，根据当前用户的所有好友的id
        for (Integer friendId : result.getData()) {
            Server.pushStatusUpdate(friendId, pushMessage);//按照该用户的好友的Id列表，挨个向其好友发送拼接后的在线状态更新消息pushMessage
        }
    }

    //
    public void markUserOfflineAndNotify(Integer userId) {
        Result result = userDao.updateUserStatus(userId, 0);//更新数据库中的用户在线状态信息

        if (result.getCode() == 0) {
            broadcastStatusUpdate(userId, 0);//按照该用户的好友的Id列表，挨个向其好友推送在线状态更新消息
        }
    }

}
