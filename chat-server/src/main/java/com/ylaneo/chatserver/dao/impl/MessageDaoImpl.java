package com.ylaneo.chatserver.dao.impl;

import com.ylaneo.chatserver.dao.MessageDao;
import com.ylaneo.chatserver.entity.ChatMessage;
import com.ylaneo.chatserver.entity.Result;
import com.ylaneo.chatserver.entity.User;
import com.ylaneo.chatserver.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageDaoImpl implements MessageDao {
    //
    @Override
    public Result<List<ChatMessage>> getAllMessage(Integer userId, Integer friendId) {
        String sql = "(select * from chat_message where sender_id=? and receiver_id=?)  " +
                "  union  " +
                "(select * from chat_message where sender_id=? and receiver_id=?)  ";
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
}
