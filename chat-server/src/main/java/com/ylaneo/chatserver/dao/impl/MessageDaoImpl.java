package com.ylaneo.chatserver.dao.impl;

import com.ylaneo.chatserver.dao.MessageDao;
import com.ylaneo.chatserver.entity.ChatMessage;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDaoImpl implements MessageDao {
    //
    @Override
    public Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId) {
        String sql = "(select * from chat_message where sender_id=? and receiver_id=?)  " +
                "  union  " +
                "(select * from chat_message where sender_id=? and receiver_id=?)   " +
                "order by create_time asc, id asc";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql)) {
            //
            pt.setInt(1, userId);
            pt.setInt(2, friendId);
            pt.setInt(3, friendId);
            pt.setInt(4, userId);
            //三个变量定义出来
            ResultSet resultSet = pt.executeQuery();
            List<ChatMessage> messageList = new ArrayList<>();
            Result<List<ChatMessage>> result = new Result<>();
            //遍历将查到的数据添加到列表中
            while (resultSet.next()) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setChatMessageId(resultSet.getInt("id"));
                chatMessage.setSenderId(resultSet.getInt("sender_id"));
                chatMessage.setReceiverId(resultSet.getInt("receiver_id"));
                chatMessage.setType(resultSet.getInt("type"));
                chatMessage.setContent(resultSet.getString("content"));
                chatMessage.setReadAt(resultSet.getObject("read_at", LocalDateTime.class));//转换为LocalDateTimel类型之后再进行赋值
                chatMessage.setStatus(resultSet.getInt("status"));
                chatMessage.setCreateTime(resultSet.getObject("create_time", LocalDateTime.class));
                chatMessage.setUpdateTime(resultSet.getObject("update_time", LocalDateTime.class));

                messageList.add(chatMessage);
            }
            //
            return Result.success(messageList);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return Result.error("数据库操作错误:" + e.getMessage());
        }
    }

    @Override
    public Result<ChatMessage> sendMessage(Integer senderId, Integer receiverId, Integer type, String content, Integer status) {
        String sql = "insert into chat_message (sender_id,receiver_id,type,content,status) values(?,?,?,?,?)";
        try (Connection conn = new DBConnection().getConnection();
//             PreparedStatement pt = conn.prepareStatement(sql)) {
//             PreparedStatement pt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {//需要这个flag，以便后面能拿自增主键：ResultSet keys = pt.getGeneratedKeys()
            //
            pt.setInt(1, senderId);
            pt.setInt(2, receiverId);
            pt.setInt(3, type);
            pt.setString(4, content);
            pt.setInt(5, status);
            //
            int rows=pt.executeUpdate();
            if(rows!=0){
                ;//insert成功就不要返回，直接执行下一步
            }else {
                return Result.error("发送消息失败");
            }
            //拿到这条新创建的消息记录的所有字段
            try (ResultSet keys = pt.getGeneratedKeys()) {
                if (keys.next()) {
                    Integer newId = keys.getInt(1);//获取这条新建的消息记录的id字段
                    //
                    ChatMessage saved = findMessageById(newId).getData();
                    return Result.success(saved);
                }
            }
            return Result.error("发送失败:未拿到主键");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return Result.error("数据库操作错误:" + e.getMessage());
        }
    }

    @Override
    public Result<ChatMessage> findMessageById(Integer messageId) {
        String sql="select * from chat_message where id=?";
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql) ){
            pt.setInt(1,messageId);
            ChatMessage chatMessage=new ChatMessage();//创建一个User实例，传给登录页面进行用户信息状态管理
            ResultSet resultSet =pt.executeQuery();
            if(resultSet.next()){
                //为user实例赋值
                chatMessage.setChatMessageId(resultSet.getInt("id"));
                chatMessage.setSenderId(resultSet.getInt("sender_id"));
                chatMessage.setReceiverId(resultSet.getInt("receiver_id"));
                chatMessage.setType(resultSet.getInt("type"));
                chatMessage.setContent(resultSet.getString("content"));
                chatMessage.setReadAt(resultSet.getObject("read_at",LocalDateTime.class));
                chatMessage.setStatus(resultSet.getInt("status"));
                chatMessage.setCreateTime(resultSet.getObject("create_time",LocalDateTime.class));
                chatMessage.setUpdateTime(resultSet.getObject("update_time",LocalDateTime.class));
                //
                return Result.success(chatMessage);//返回查找到的信息给
            }
            else{
                return Result.error("根据id查询消息对象失败");
            }
        }
        //异常处理
        catch (SQLException e){
            System.out.println("数据库操作错误"+e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
