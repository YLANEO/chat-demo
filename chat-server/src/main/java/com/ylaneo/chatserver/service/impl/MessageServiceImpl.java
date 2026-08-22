package com.ylaneo.chatserver.service.impl;

import com.ylaneo.chatserver.dao.ContactDao;
import com.ylaneo.chatserver.dao.MessageDao;
import com.ylaneo.chatserver.dao.impl.ContactDaoImpl;
import com.ylaneo.chatserver.dao.impl.MessageDaoImpl;
import com.ylaneo.chatserver.entity.ChatMessage;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.service.MessageService;
import com.ylaneo.chatserver.socket.Server;
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
                //直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
                String readAtStr = messageList.get(i).getReadAt() == null ? "" : messageList.get(i).getReadAt().toString();
                response = response + "<" + readAtStr + ">";
//                response=response+"<"+messageList.get(i).getReadAt()+">";
                response=response+"<"+messageList.get(i).getStatus()+">";
                //直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
                String createTimeStr = messageList.get(i).getCreateTime() == null ? "" : messageList.get(i).getCreateTime().toString();
                response = response + "<" + createTimeStr + ">";
//                response=response+"<"+messageList.get(i).getCreateTime()+">";
                //直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
                String updateTimeStr = messageList.get(i).getUpdateTime() == null ? "" : messageList.get(i).getUpdateTime().toString();
                response = response + "<" + updateTimeStr + ">";
//                response=response+"<"+messageList.get(i).getUpdateTime()+">";
            }
        }else {
            response=response+"<"+r.getCode()+">";
            response=response+"<"+r.getMessage()+">";
        }
        return response;
    }

    @Override
    public String sendMessage(String request) {
        List<String> params=requestHandler.handleParams(request);
        Integer senderId=Integer.valueOf(params.get(1));
        Integer receiverId=Integer.valueOf(params.get(2));
        Integer type=Integer.valueOf(params.get(3));
        String content=params.get(4);
        Integer status=Integer.valueOf(params.get(5));
        //获取新增的这条消息对象
        Result<ChatMessage> r=messageDao.sendMessage(senderId,receiverId,type,content,status);
        //
        if(r.getCode()==0){
            ChatMessage chatMessage=r.getData();
            String response="<"+r.getCode()+">"+"<"+r.getMessage()+">"+"<"+chatMessage.getChatMessageId()+">"+
                    "<"+chatMessage.getSenderId()+">"+"<"+chatMessage.getReceiverId()+">"+"<"+chatMessage.getType()+">"+
                    "<"+chatMessage.getContent()+">"+
                    "<"+( chatMessage.getReadAt()==null?"":chatMessage.getReadAt() )+">"+//直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
                    "<"+chatMessage.getStatus()+">"+
                    "<"+( chatMessage.getCreateTime()==null?"":chatMessage.getCreateTime() )+">"+//直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
                    "<"+( chatMessage.getUpdateTime()==null?"":chatMessage.getUpdateTime() )+">";//直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
            //进行将这条消息只推送给接收方
            if (r.getCode() == 0) {
                //只推送消息给接受者(receiverId)，告诉他发送消息的好友的id以及新增的消息对象的id。不用推送消息信息给发送者，因为发送者可以直接拿到返回的消息对象。
                Server.pushStatusUpdate(receiverId, "<push><messageAdd>"+"<"+senderId+">"+"<"+chatMessage.getChatMessageId()+">");
            }
            //
            return response;
        }else{
            String response="<"+r.getCode()+">"+"<"+r.getMessage()+">";
            return response;
        }
    }

    @Override
    public String findMessageById(String request) {
        List<String>params=requestHandler.handleParams(request);
        Integer messageId=Integer.valueOf(params.get(1));
        Result<ChatMessage> r=messageDao.findMessageById(messageId);
        //
        if(r.getCode()==0){
            ChatMessage chatMessage=r.getData();
            String response="<"+r.getCode()+">"+"<"+r.getMessage()+">"+"<"+chatMessage.getChatMessageId()+">"+
                    "<"+chatMessage.getSenderId()+">"+"<"+chatMessage.getReceiverId()+">"+"<"+chatMessage.getType()+">"+
                    "<"+chatMessage.getContent()+">"+
                    "<"+( chatMessage.getReadAt()==null?"":chatMessage.getReadAt() )+">"+//直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
                    "<"+chatMessage.getStatus()+">"+
                    "<"+( chatMessage.getCreateTime()==null?"":chatMessage.getCreateTime() )+">"+//直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
                    "<"+( chatMessage.getUpdateTime()==null?"":chatMessage.getUpdateTime() )+">";//直接传null会导致客户端接收到的是字符串"null",而非真正的空字符串，所以这里需要先进行判断，
            return response;
        }else{
            String response="<"+r.getCode()+">"+"<"+r.getMessage()+">";
            return response;
        }
    }


}
