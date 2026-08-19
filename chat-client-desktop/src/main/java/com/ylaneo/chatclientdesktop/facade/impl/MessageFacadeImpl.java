package com.ylaneo.chatclientdesktop.facade.impl;

import com.ylaneo.chatclientdesktop.entity.ChatMessage;
import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.MessageFacade;
import com.ylaneo.chatclientdesktop.net.RequestSender;
import com.ylaneo.chatclientdesktop.util.ResponseHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageFacadeImpl implements MessageFacade {
    RequestSender requestSender=new RequestSender();
    ResponseHandler responseHandler=new ResponseHandler();
    //
    @Override
    public Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId) {
        //通过socket获得字符串格式的数据
        String request="<getAllMessage>"+"<"+userId+">"+"<"+friendId+">";//组装request从requestsender拿到service层中
        String response;//用于接收数据
        try {
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //将列表中的数据取出，并将String类型转为正确的数据类型
        int code=Integer.valueOf(r.get(0)) ;//响应码，根据其不同对应之后的不同操作
        String message=r.get(1);//响应信息
        Integer countList=Integer.valueOf(r.get(2));//有几条数据
        if(code==0){
            //创建一个list对象，进行赋值，用于封装到Result中
            List<ChatMessage>messageList=new ArrayList<>();
            //从第3个序号开始将response中解析出来的信息放入对应的对象中去
            for(int i=0;i<countList;i++){
                ChatMessage chatMessage=new ChatMessage();
//                int j=i*4+3;
                chatMessage.setChatMessageId(Integer.valueOf(r.get(9*i+3)));
                chatMessage.setSenderId(Integer.valueOf(r.get(9*i+4)));
                chatMessage.setReceiverId(Integer.valueOf(r.get(9*i+5)));
                chatMessage.setType(Integer.valueOf(r.get(9*i+6)));
                chatMessage.setContent(r.get(9*i+7));
                //
                String readAtString=r.get(9*i+8);
                chatMessage.setReadAt(readAtString==null || readAtString.isEmpty()  ? null: LocalDateTime.parse(readAtString));
                //
                chatMessage.setStatus(Integer.valueOf(r.get(9*i+9)));
                //
                String createTimeString=r.get(9*i+10);
                chatMessage.setCreateTime(createTimeString==null || createTimeString.isEmpty()  ? null: LocalDateTime.parse(createTimeString));
                //
                String updateTimeString=r.get(9*i+11);
                chatMessage.setUpdateTime(updateTimeString==null || updateTimeString.isEmpty()  ? null: LocalDateTime.parse(updateTimeString));
                //
                messageList.add(chatMessage);
            }
            return Result.success(messageList);
        }else {
            //
            return Result.error("获取消息列表信息失败:"+message);
        }
    }
    //
}
