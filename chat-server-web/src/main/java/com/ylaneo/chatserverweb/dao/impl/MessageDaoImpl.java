package com.ylaneo.chatserverweb.dao.impl;


import com.ylaneo.chatserverweb.dao.MessageDao;
import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.util.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
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

    /**
     * 向上分页查询历史消息。
     * lastMessageId = null 时表示最新一页；否则查询 id < lastMessageId 的消息。
     * 返回的消息按时间倒序，再用 list 倒置回来，方便展示。
     */
    @Override
    public Result<List<ChatMessage>> getHistoryMessage(Integer userId, Integer friendId, Long lastMessageId, Integer limit) {
        String sql = "select * from chat_message where ((sender_id=? and receiver_id=?) or (sender_id=? and receiver_id=?)) " ;
        if (lastMessageId != null) {
            sql=sql+"and id < ? ";
        }
        sql=sql+"order by id desc limit ?";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            pt.setInt(idx++, userId);
            pt.setInt(idx++, friendId);
            pt.setInt(idx++, friendId);
            pt.setInt(idx++, userId);
            if (lastMessageId != null) {
                pt.setLong(idx++, lastMessageId);
            }
            pt.setInt(idx, limit != null && limit > 0 ? limit : 20);
            ResultSet rs = pt.executeQuery();
            List<ChatMessage> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            // 倒序转正序，便于前端展示
            java.util.Collections.reverse(list);
            return Result.success(list);
        } catch (SQLException e) {
            return Result.error("数据库操作错误:" + e.getMessage());
        }
    }
    /**
     * 把"对方发给我的、还没读的"消息批量置为已读。
     * 只标对方发给我的（sender_id=friendId, receiver_id=userId），不标自己发的。
     */
    @Override
    public Result<Integer> markAsRead(Integer userId, Integer friendId) {
        String sql = "update chat_message set read_at=now() where sender_id=? and receiver_id=? and read_at is null ";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql)) {
            pt.setInt(1, friendId);
            pt.setInt(2, userId);
            int rows = pt.executeUpdate();
            return Result.success(rows);
        } catch (SQLException e) {
            return Result.error("数据库操作错误:" + e.getMessage());
        }
    }
    @Override
    public Result<Integer> countUnread(Integer userId, Integer friendId) {
        String sql = "SELECT COUNT(*) AS cnt FROM chat_message " +
                "WHERE sender_id=? AND receiver_id=? AND read_at IS NULL";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql)) {
            pt.setInt(1, friendId);
            pt.setInt(2, userId);
            ResultSet rs = pt.executeQuery();
            int cnt = 0;
            if (rs.next()) cnt = rs.getInt("cnt");
            return Result.success(cnt);
        } catch (SQLException e) {
            return Result.error("数据库操作错误:" + e.getMessage());
        }
    }
    @Override
    public Result<Integer> countTotalUnread(Integer userId) {
        String sql = "SELECT COUNT(*) AS cnt FROM chat_message " +
                "WHERE receiver_id=? AND read_at IS NULL";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql)) {
            pt.setInt(1, userId);
            ResultSet rs = pt.executeQuery();
            int cnt = 0;
            if (rs.next()) cnt = rs.getInt("cnt");
            return Result.success(cnt);
        } catch (SQLException e) {
            return Result.error("数据库操作错误:" + e.getMessage());
        }
    }
    // 抽出公共的字段映射
    private ChatMessage mapRow(ResultSet rs) throws SQLException {
        ChatMessage m = new ChatMessage();
        m.setChatMessageId(rs.getInt("id"));
        m.setSenderId(rs.getInt("sender_id"));
        m.setReceiverId(rs.getInt("receiver_id"));
        m.setType(rs.getInt("type"));
        m.setContent(rs.getString("content"));
        m.setReadAt(rs.getObject("read_at", LocalDateTime.class));
        m.setStatus(rs.getInt("status"));
        m.setCreateTime(rs.getObject("create_time", LocalDateTime.class));
        m.setUpdateTime(rs.getObject("update_time", LocalDateTime.class));
        return m;
    }
}
