package com.ylaneo.chatserver.service.impl;

import com.ylaneo.chatserver.dao.ContactDao;
import com.ylaneo.chatserver.dao.UserDao;
import com.ylaneo.chatserver.dao.impl.ContactDaoImpl;
import com.ylaneo.chatserver.dao.impl.UserDaoImpl;
import com.ylaneo.chatserver.dto.FriendRequestDto;
import com.ylaneo.chatserver.entity.FriendRequest;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.service.ContactService;
import com.ylaneo.chatserver.util.RequestHandler;

import java.util.ArrayList;
import java.util.List;

public class ContactServiceImpl implements ContactService {

    RequestHandler requestHandler=new RequestHandler();
    ContactDao contactDao=new ContactDaoImpl();

    @Override
    public String getFriendList(String request) {
        List<String> params=requestHandler.handleParams(request);
        Integer currentUserId=Integer.valueOf(params.get(1));
        String response="";
        Result r=contactDao.getFriendList(currentUserId);
        //
        if(r.getCode()==0){
            List<User> friendList=(List<User>)r.getData();
//            friendList=(List<User>)r.getData();
            int countList=friendList.size();
            //
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
            response=response+"<"+countList+">";
            for(int i=0;i<countList;i++){
                //不要忘记加尖括号<>
                response=response+"<"+friendList.get(i).getUsername()+">";
                response=response+"<"+friendList.get(i).getNickname()+">";
                response=response+"<"+friendList.get(i).getAvatar_url()+">";
                response=response+"<"+friendList.get(i).getStatus()+">";
            }
        }else {
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
        }
        return response;
    }

    @Override
    public String searchUserByKeyword(String request) {
        List<String> params=requestHandler.handleParams(request);
        String keyword=params.get(1);
        Integer currentUserId=Integer.valueOf(params.get(2));
        String response="";
        Result r=contactDao.searchUserByKeyword(keyword,currentUserId);
        //
        if(r.getCode()==0){
            List<User> friendList=(List<User>)r.getData();
//            friendList=(List<User>)r.getData();
            int countList=friendList.size();
            //
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
            response=response+"<"+countList+">";
            for(int i=0;i<countList;i++){
                //不要忘记加尖括号<>
                response=response+"<"+friendList.get(i).getUserid()+">";
                response=response+"<"+friendList.get(i).getUsername()+">";
                response=response+"<"+friendList.get(i).getNickname()+">";
                response=response+"<"+friendList.get(i).getAvatar_url()+">";
                response=response+"<"+friendList.get(i).getStatus()+">";
            }
        }else {
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
        }
        return response;
    }

    @Override
    public String sendFriendRequest(String request) {
        //1.解析出各个参数
        List<String>params=requestHandler.handleParams(request);
        Integer senderId=Integer.valueOf(params.get(1));//
        Integer receiverId=Integer.valueOf(params.get(2));
        String message=params.get(3);
        //2.调用dao的接口获得result对象数据
        Result<User> r1=contactDao.sendFriendRequest(senderId,receiverId,message);
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
    public String getFriendRequests(String request) {
        List<String> params=requestHandler.handleParams(request);
        Integer receiverId=Integer.valueOf(params.get(1));
        String response="";
        Result r=contactDao.getFriendRequests(receiverId);
        //
        if(r.getCode()==0){
            List<FriendRequestDto> friendRequestDtoList=(List<FriendRequestDto>)r.getData();
//            friendList=(List<User>)r.getData();
            int countList=friendRequestDtoList.size();
            //
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
            response=response+"<"+countList+">";
            for(int i=0;i<countList;i++){
                //不要忘记加尖括号<>
                response=response+"<"+friendRequestDtoList.get(i).getRequestId()+">";
                response=response+"<"+friendRequestDtoList.get(i).getSenderId()+">";
                response=response+"<"+friendRequestDtoList.get(i).getSenderUsername()+">";
                response=response+"<"+friendRequestDtoList.get(i).getSenderNickname()+">";
                response=response+"<"+friendRequestDtoList.get(i).getSenderAvatar()+">";
                response=response+"<"+friendRequestDtoList.get(i).getMessage()+">";
                response=response+"<"+friendRequestDtoList.get(i).getStatus()+">";
                response=response+"<"+friendRequestDtoList.get(i).getCreateTime()+">";
            }
        }else {
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
        }
        return response;
    }
}
