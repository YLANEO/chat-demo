package com.ylaneo.chatclientdesktop.facade.impl;

import com.ylaneo.chatclientdesktop.dto.FriendRequestDto;
import com.ylaneo.chatclientdesktop.entity.FriendRequest;
import com.ylaneo.chatclientdesktop.entity.Result;
import com.ylaneo.chatclientdesktop.entity.User;
import com.ylaneo.chatclientdesktop.facade.ContactFacade;
import com.ylaneo.chatclientdesktop.net.RequestSender;
import com.ylaneo.chatclientdesktop.util.ResponseHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ContactFacadeImpl implements ContactFacade {
    RequestSender requestSender=new RequestSender();
    ResponseHandler responseHandler=new ResponseHandler();
    //
    @Override
    public Result<List<User>> getFriendList(Integer currentUserId) {
        //通过socket获得字符串格式的数据
        String request="<getFriendList>"+"<"+currentUserId+">";//组装request从requestsender拿到service层中
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
            List<User>friendList=new ArrayList<>();
            //从第3个序号开始将response中解析出来的信息放入对应的对象中去
            for(int i=0;i<countList;i++){
                User user=new User();
//                int j=i*4+3;
                user.setUserid(Integer.valueOf(r.get(5*i+3)));
                user.setUsername(r.get(5*i+4));
                user.setNickname(r.get(5*i+5));
                user.setAvatar_url(r.get(5*i+6));
                user.setStatus(Integer.valueOf(r.get(5*i+7)));
                friendList.add(user);
            }
            return Result.success(friendList);
        }else {
            //
            return Result.error("获取好友列表信息失败:"+message);
        }
    }

    @Override
    public Result<List<User>> searchUserByKeyword(String keyword, Integer currentUserId) {
        //通过socket获得字符串格式的数据
        String request="<searchUserByKeyword>"+"<"+keyword+">"+"<"+currentUserId+">";//组装request从requestsender拿到service层中
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
            List<User>friendList=new ArrayList<>();
            //从第3个序号开始将response中解析出来的信息放入对应的对象中去
            for(int i=0;i<countList;i++){
                User user=new User();
//                int j=i*4+3;
                user.setUserid(Integer.valueOf(r.get(5*i+3)));
                user.setUsername(r.get(5*i+4));
                user.setNickname(r.get(5*i+5));
                user.setAvatar_url(r.get(5*i+6));
                user.setStatus(Integer.valueOf(r.get(5*i+7)));
                friendList.add(user);
            }
            return Result.success(friendList);
        }else {
            //
            return Result.error("获取好友列表信息失败:"+message);
        }
    }

    @Override
    public Result sendFriendRequest(Integer senderId, Integer receiverId, String message) {
        //发送好友申请前，进行验证二者是否已经是好友
        if(isFriend(senderId,receiverId).getData()==1){
            return Result.error("已经是好友，无需重复添加");
        }
        //检查两者之间是否已经存在已经发送并且没有处理的好友申请
        Result<Boolean> sentByMe = hasPendingRequest(senderId, receiverId);
        Result<Boolean> sentToMe = hasPendingRequest(receiverId, senderId);
        if(sentByMe.getData()){
            return Result.error("已向该用户发送申请，请勿重复发送");
        }
        if(sentToMe.getData()){
            return Result.error("已收到待处理申请，请先进行处理");
        }
        String request="<sendFriendRequest>"+"<"+senderId+">"+"<"+receiverId+">"+"<"+message+">";
        String response;//用于接收数据
        try {
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将response从String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //将列表中的数据取出，并将String类型转为正确的数据类型
        int code=Integer.valueOf(r.get(0)) ;
        String message_result=r.get(1);
        if(code==0){
            return Result.success("发送成功:"+message_result);
        }else {
            //
            return Result.error("发送失败:"+message_result);
        }
    }

    @Override
    public Result<List<FriendRequestDto>> getFriendRequests(Integer receiverId) {
        //通过socket获得字符串格式的数据
        String request="<getFriendRequests>"+"<"+receiverId+">";//组装request从requestsender拿到service层中
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
            List<FriendRequestDto>requestList=new ArrayList<>();
            //从第3个序号开始将response中解析出来的信息放入对应的对象中去
            for(int i=0;i<countList;i++){
                FriendRequestDto friendRequestDto=new FriendRequestDto();
                //为该对象的 各个属性赋值
                friendRequestDto.setRequestId(Integer.valueOf(r.get(8*i+3)));
                friendRequestDto.setSenderId(Integer.valueOf(r.get(8*i+4)));
                friendRequestDto.setSenderUsername(r.get(8*i+5));
                friendRequestDto.setSenderNickname(r.get(8*i+6));
                friendRequestDto.setSenderAvatar(r.get(8*i+7));
                friendRequestDto.setMessage(r.get(8*i+8));
                friendRequestDto.setStatus(Integer.valueOf(r.get(8*i+9)));
                //这里处理LocalDateTime类
                String requestCreatetimeStr=r.get(8*i+10);
                friendRequestDto.setCreateTime(requestCreatetimeStr==null||requestCreatetimeStr.isEmpty()? null : LocalDateTime.parse(requestCreatetimeStr));
                requestList.add(friendRequestDto);
            }
            return Result.success(requestList);
        }else {
            //
            return Result.error("获取好友申请列表信息失败:"+message);
        }
    }

    @Override
    public Result handleRequest(Integer friendRequestId, Integer senderId, Integer receiverId, Integer status) {
        String request="<handleRequest>"+"<"+friendRequestId+">"+"<"+senderId+">"+"<"+receiverId+">"+"<"+status+">";
        String response;//用于接收数据
        try {
            response= requestSender.sendRequest(request);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        //将response从String转换为列表形式
        List<String> r=responseHandler.handleParams(response);
        //将列表中的数据取出，并将String类型转为正确的数据类型
        Integer code=Integer.valueOf(r.get(0)) ;
        String message=r.get(1);
        if(code==0){
            return Result.success(message);
        }else {
            //
            return Result.error(message);
        }
    }

    @Override
    public Result<Integer> isFriend(Integer userId, Integer friendId) {
        String request="<isFriend>"+"<"+userId+">"+"<"+friendId+">";
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
            Integer isFriendParam=Integer.valueOf(r.get(2));
            return Result.success(isFriendParam);
        }else {
            return Result.error(message);
        }
    }

    @Override
    public Result<Boolean> hasPendingRequest(Integer senderId, Integer receiverId) {
        String request="<hasPendingRequest>"+"<"+senderId+">"+"<"+receiverId+">";
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
//            Boolean hasPendingRequestParam=Boolean.getBoolean(r.get(2));
            Boolean hasPendingRequestParam=Boolean.parseBoolean(r.get(2));//
            return Result.success(hasPendingRequestParam);
        }else {
            return Result.error(message);
        }
    }

    @Override
    public Result<List<Integer>> getFriendIds(Integer userId) {
        String request="<getFriendIds>"+"<"+userId+">";
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
        Integer countList=Integer.valueOf(r.get(2));//有几条数据
        if(code==0){
            List<Integer>friendIdList=new ArrayList<>();
            for(int i=0;i<countList;i++){
                Integer id=Integer.valueOf(r.get(i+3));
                friendIdList.add(id);
            }
            return Result.success(friendIdList);
        }else {
            return Result.error(message);
        }
    }

}
