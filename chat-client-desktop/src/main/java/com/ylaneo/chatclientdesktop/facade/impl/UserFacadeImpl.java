package com.ylaneo.chatclientdesktop.facade.impl;


import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.UserFacade;
import com.ylaneo.chatclientdesktop.net.RequestSender;
import com.ylaneo.chatclientdesktop.util.ResponseHandler;

import java.io.IOException;
import java.util.List;


public class UserFacadeImpl implements UserFacade {

    // ======================================== 感觉这两个拿出来复用好一点？？？ ========================================
    RequestSender requestSender=new RequestSender();
    ResponseHandler responseHandler=new ResponseHandler();
    // ======================================== 接口实现类具体内容 ========================================

    //login
    @Override
    public Result<User> login(String username, String password) {
        //通过socket获得字符串格式的数据
        String request="<login>"+"<"+username+">"+"<"+password+">";//组装request从requestsender拿到service层中
        String response;//用于接收数据
        try {
            //直接把requestsender中的这块内容拿到service层中来处理 response= requestSender.login(username,password);
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //将列表中的数据取出，并将String类型转为正确的数据类型
        int code=Integer.valueOf(r.get(0)) ;
        String message=r.get(1);
        if(code==0){
            //这里注意
            int userid=Integer.valueOf(r.get(2));
            //创建一个user对象，并进行赋值，用于封装到Result中
            User user=new User();
            user.setUserid(userid);
            user.setUsername(username);
            return Result.success(user);
        }else {
            //
            return Result.error("登录失败:"+message);
        }
    }

    //register
    @Override
    public Result register(String username, String password, String repassword) {
        String request="<register>"+"<"+username+">"+"<"+password+">"+"<"+repassword+">";
        String response;//用于接收数据
        try {
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //将列表中的数据取出，并将String类型转为正确的数据类型
        int code=Integer.valueOf(r.get(0)) ;
        String message=r.get(1);
        if(code==0){
            return Result.success("注册成功:"+message);
        }else {
            //
            return Result.error("注册失败:"+message);
        }
    }

    //更新用户名
    @Override
    public Result updateUsername(Integer userId, String reUsername) {
        String request="<updateUsername>"+"<"+userId+">"+"<"+reUsername+">";
        String response;//用于接收数据
        try {
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //
        int code=Integer.valueOf(r.get(0));
        String message=r.get(1);
        //
        if(code==0){
            return Result.success("更新成功: "+message);
        }else {
            return Result.error("更新失败: "+message);
        }
    }

    //根据id获得用户名
    @Override
    public Result<User> getUsername(Integer userId) {
        String request="<getUsername>"+"<"+userId+">";
        String response;//用于接收数据
        try {
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //
        //将列表中的数据取出，并将String类型转为正确的数据类型
        int code=Integer.valueOf(r.get(0)) ;
        String message=r.get(1);
        if(code==0){
            //这里注意
            int userid=Integer.valueOf(r.get(2));
            String username=r.get(3);
            //创建一个user对象，并进行赋值，用于封装到Result中
            User user=new User();
            user.setUserid(userid);
            user.setUsername(username);
            return Result.success(user);
        }else {
            //
            return Result.error("根据id获取用户名失败:"+message);
        }
    }

    @Override
    public Result updateUserStatus(Integer userId, Integer userStatus) {
        String request="<updateUserStatus>"+"<"+userId+">"+"<"+userStatus+">";
        String response;
        try{
            response=requestSender.sendRequest(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //
        List<String>r=responseHandler.handleParams(response);
        Integer code=Integer.valueOf(r.get(0));
        String message=r.get(1);
        if(code==0){
            return Result.success(message);
        }else {
            return Result.error(message);
        }
    }

    @Override
    public Result updateUserAvatar(Integer userId,String userAvatarUrl) {
        String request="<updateUserAvatar>"+"<"+userId+">"+"<"+userAvatarUrl+">";
        String response;//用于接收数据
        try {
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //
        int code=Integer.valueOf(r.get(0));
        String message=r.get(1);
        //
        if(code==0){
            return Result.success("上传成功: "+message);
        }else {
            return Result.error("上传失败: "+message);
        }
    }

    @Override
    public Result updateNickname(Integer userId, String newNickname) {
        String request="<updateNickname>"+"<"+userId+">"+"<"+newNickname+">";
        String response;
        try{
            response=requestSender.sendRequest(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //
        List<String>r=responseHandler.handleParams(response);
        Integer code=Integer.valueOf(r.get(0));
        String message=r.get(1);
        if(code==0){
            return Result.success(message);
        }else {
            return Result.error(message);
        }
    }

    @Override
    public Result<User> getUserData(Integer userId) {
        String request="<getUserData>"+"<"+userId+">";
        String response;
        try{
            response=requestSender.sendRequest(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //
        List<String>r=responseHandler.handleParams(response);
        Integer code=Integer.valueOf(r.get(0));
        String message=r.get(1);
        if(code==0){
            User user=new User();
            //
            String username=r.get(2);
            String nickname=r.get(3);
            String userAvatarUrl=r.get(4);
            Integer status=Integer.valueOf(r.get(5));
            //
            user.setUserid(userId);
            user.setUsername(username);
            user.setNickname(nickname);
            user.setAvatar_url(userAvatarUrl);
            user.setStatus(status);
            //
            return Result.success(user);
        }else {
            return Result.error(message);
        }
    }
}
