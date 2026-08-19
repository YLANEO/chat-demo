package com.ylaneo.chatserver.service.impl;

import com.ylaneo.chatserver.dao.ContactDao;
import com.ylaneo.chatserver.dao.MessageDao;
import com.ylaneo.chatserver.dao.impl.ContactDaoImpl;
import com.ylaneo.chatserver.dao.impl.MessageDaoImpl;
import com.ylaneo.chatserver.entity.ChatMessage;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.service.MessageService;
import com.ylaneo.chatserver.util.RequestHandler;

import java.util.List;

public class MessageServiceImpl implements MessageService {

    RequestHandler requestHandler=new RequestHandler();
    MessageDao messageDao=new MessageDaoImpl();
    //
    @Override
    public String getAllMessage(String request) {
        List<String> params=requestHandler.handleParams(request);
        Integer userId=Integer.valueOf(params.get(1));
        Integer friendId=Integer.valueOf(params.get(2));
        String response="";
        Result r=messageDao.getAllMessage(userId,friendId);
        //
        if(r.getCode()==0){
            List<ChatMessage> messageList=(List<ChatMessage>)r.getData();
//            friendList=(List<User>)r.getData();
            int countList=messageList.size();
            //
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
            response=response+"<"+countList+">";
            for(int i=0;i<countList;i++){
                //不要忘记加尖括号<>
                response=response+"<"+messageList.get(i).getChatMessageId()+">";
                response=response+"<"+messageList.get(i).getSenderId()+">";
                response=response+"<"+messageList.get(i).getReceiverId()+">";
                response=response+"<"+messageList.get(i).getType()+">";
                response=response+"<"+messageList.get(i).getContent()+">";
                response=response+"<"+messageList.get(i).getReadAt()+">";
                response=response+"<"+messageList.get(i).getStatus()+">";
                response=response+"<"+messageList.get(i).getCreateTime()+">";
                response=response+"<"+messageList.get(i).getUpdateTime()+">";
            }
        }else {
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
        }
        return response;
    }
}
