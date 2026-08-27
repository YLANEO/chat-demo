package com.ylaneo.chatserverweb.dao.impl;



import com.ylaneo.chatserverweb.dao.ContactDao;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.dto.FriendRequestDto;
import com.ylaneo.chatserverweb.util.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ContactDaoImpl implements ContactDao {
    @Override
    public Result<List<User>> getFriendList(Integer currentUserId) {
        String sql="select u.id,u.username,u.nickname,u.avatar_url,u.status " +
                "from user u " +
                "inner join friend_relation fr on u.id = fr.friend_id " + //这里应该是fr.friend_id而不是fr.user_id
                "where fr.user_id=? and fr.status=1";
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql)){
            //
            pt.setInt(1,currentUserId);
            //三个变量定义出来
            ResultSet resultSet =pt.executeQuery();
            List<User> friendList=new ArrayList<>();
            Result<List<User>> result=new Result<>();
            //遍历将查到的数据添加到列表中
            while(resultSet.next()){
                User user=new User();
                user.setUserid(resultSet.getInt("id"));
                user.setUsername(resultSet.getString("username"));
                user.setNickname(resultSet.getString("nickname"));
                user.setAvatar_url(resultSet.getString("avatar_url"));
                user.setStatus(resultSet.getInt("status"));
                friendList.add(user);
            }
            //
            return Result.success(friendList);
        }catch (SQLException e){
            System.out.println(e.getMessage());
            return Result.error("数据库操作错误:"+e.getMessage());
        }
    }

    @Override
    public Result<List<User>> searchUserByKeyword(String keyword, Integer currentUserId) {
        String sql = "select id, username, nickname, avatar_url, status from user where (nickname like ? or username like ?) and id != ?";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql)) {
            pt.setString(1, "%" + keyword + "%");//昵称模糊匹配
            pt.setString(2, "%" + keyword + "%");//用户名精准匹配
            pt.setInt(3, currentUserId);//排除自己
            ResultSet resultSet = pt.executeQuery();
            List<User> userList = new ArrayList<>();
            while (resultSet.next()) {
                User u = new User();
                u.setUserid(resultSet.getInt("id"));
                u.setUsername(resultSet.getString("username"));
                u.setNickname(resultSet.getString("nickname"));
                u.setAvatar_url(resultSet.getString("avatar_url"));
                u.setStatus(resultSet.getInt("status"));
                userList.add(u);
            }
            //
            return Result.success(userList);
        } catch (SQLException e) {
            return Result.error("数据库错误:" + e.getMessage());
        }
    }

    @Override
    public Result sendFriendRequest(Integer senderId, Integer receiverId, String message) {
        String sql="insert into friend_request (sender_id,receiver_id,message) values (?,?,?)";
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql);){
            //
            pt.setInt(1,senderId);
            pt.setInt(2,receiverId);
            pt.setString(3,message);
            //
            int rows=pt.executeUpdate();
            if(rows!=0){
                return Result.success("发送成功");
            }else {
                return Result.error("发送失败");
            }
        }catch (SQLException e){
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<List<FriendRequestDto>> getFriendRequests(Integer receiverId) {
        //要返回发送申请者的头像，所以需要连表查询
        String sql="select fr.id as frId,fr.sender_id as senderId,u.username,u.nickname,u.avatar_url,fr.message as frMessage,fr.status,fr.create_time " +
                "from friend_request fr " +
                "inner join user u on fr.sender_id=u.id " +
                "where (receiver_id=? and fr.status=0 ) ";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql)) {
            pt.setInt(1,receiverId);
            ResultSet resultSet = pt.executeQuery();
            List<FriendRequestDto> friendRequestDtoList = new ArrayList<>();
            while (resultSet.next()) {
                User u = new User();
                FriendRequestDto friendRequestDto=new FriendRequestDto();
                friendRequestDto.setRequestId(resultSet.getInt("frId"));
                friendRequestDto.setSenderId(resultSet.getInt("senderId"));
                friendRequestDto.setSenderUsername(resultSet.getString("username"));
                friendRequestDto.setSenderNickname(resultSet.getString("nickname"));
                friendRequestDto.setSenderAvatar(resultSet.getString("avatar_url"));
                friendRequestDto.setMessage(resultSet.getString("frMessage"));
                friendRequestDto.setStatus(resultSet.getInt("status"));
                friendRequestDto.setCreateTime(resultSet.getObject("create_time", LocalDateTime.class));//
                friendRequestDtoList.add(friendRequestDto);
            }
            //
            return Result.success(friendRequestDtoList);
        } catch (SQLException e) {
            return Result.error("数据库错误:" + e.getMessage());
        }
    }

    @Override
    public Result handleRequest(Integer friendRequestId, Integer senderId, Integer receiverId, Integer status) {
        /* 一个接口包含多个数据库操作的情况 */
        String updateRequestSql = "update friend_request set status=?,update_time=now() where id=? and status=0";//忘记更新时间戳了。补上
        String insertRelationSql = "insert into friend_relation (user_id, friend_id, status) values (?, ?, 1)";
        //
        Connection conn = null;
        try {
            conn = new DBConnection().getConnection();
            conn.setAutoCommit(false);//开启事务，关闭自动提交事务

            //更新 friend_request的status字段
            try (PreparedStatement pt = conn.prepareStatement(updateRequestSql)) {
                pt.setInt(1, status);
                pt.setInt(2, friendRequestId);
                int rows = pt.executeUpdate();
                if (rows == 0) {
                    conn.rollback();//操作失败，执行回滚
                    return Result.error("申请不存在或已被处理");
                }
            }

            //接受好友申请，插入两条记录
            if (status == 1) {
                try (PreparedStatement pt = conn.prepareStatement(insertRelationSql)) {
                    pt.setInt(1, senderId);
                    pt.setInt(2, receiverId);
                    pt.executeUpdate();
                }//如果失败，异常抛到外层，被外层catch捕获
                try (PreparedStatement pt = conn.prepareStatement(insertRelationSql)) {
                    pt.setInt(1, receiverId);
                    pt.setInt(2, senderId);
                    pt.executeUpdate();
                }//如果失败，异常抛到外层，被外层catch捕获
            }

            conn.commit();//全部操作成功，则提交事务
            return Result.success("操作成功");

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); }//如果抛出异常，统一回滚
                catch (SQLException ignored) {}
            }
            return Result.error("数据库错误:" + e.getMessage());

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);//无论成功失败，都要恢复自动提交
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Result<Integer> isFriend(Integer userId, Integer friendId) {
        String sql="select status from friend_relation where user_id=? and friend_id=? ";
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql)){
            //
            pt.setInt(1,userId);
            pt.setInt(2,friendId);
            //
            ResultSet resultSet=pt.executeQuery();
            Integer isFriendParam=-1;
            if (resultSet.next()) {
                isFriendParam = resultSet.getInt("status");
            }
            return Result.success(isFriendParam);
        }catch (SQLException e){
            return Result.error("数据库错误:" + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> hasPendingRequest(Integer senderId, Integer receiverId) {
        //到底是单向验证好还是双向验证？细化区分业务的话，单向更好？
//        String sql="select 1 from friend_request where ( ( (sender_id=? and receiver_id=?) or (receiver_id=? and sender_id=?) ) and status=0 ) ";
        String sql="select 1 from friend_request where (  (sender_id=? and receiver_id=?)   and status=0 ) ";
        try(Connection conn=new DBConnection().getConnection();
            PreparedStatement pt=conn.prepareStatement(sql)){
            //
            pt.setInt(1,senderId);
            pt.setInt(2,receiverId);
//            pt.setInt(3,receiverId);
//            pt.setInt(4,senderId);
            ResultSet resultSet=pt.executeQuery();
            Boolean hasPendingRequestParam=resultSet.next();//这样就可以返回布尔值，
//            if(resultSet.next()){
//                hasPendingRequestParam=resultSet.getInt("status");
//            }
            return Result.success(hasPendingRequestParam);
        }catch (SQLException e){
            return Result.error("数据库错误:" + e.getMessage());
        }
    }

    @Override
    public Result<List<Integer>> getFriendIds(Integer userId) {
        String sql = "select friend_id from friend_relation where user_id=? and status=1";
        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement pt = conn.prepareStatement(sql)) {
            pt.setInt(1, userId);
            ResultSet resultSet = pt.executeQuery();
            List<Integer> friendIds = new ArrayList<>();
            while (resultSet.next()) {
                friendIds.add(resultSet.getInt("friend_id"));
            }
            return Result.success(friendIds);
        } catch (SQLException e) {
            return Result.error("数据库错误:" + e.getMessage());
        }
    }

}
