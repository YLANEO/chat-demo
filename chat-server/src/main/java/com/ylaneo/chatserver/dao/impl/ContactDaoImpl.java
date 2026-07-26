package com.ylaneo.chatserver.dao.impl;

import com.ylaneo.chatserver.dao.ContactDao;
import com.ylaneo.chatserver.dto.FriendRequestDto;
import com.ylaneo.chatserver.entity.FriendRequest;
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

public class ContactDaoImpl implements ContactDao {
    @Override
    public Result<List<User>> getFriendList(Integer currentUserId) {
        String sql="select u.username,u.nickname,u.avatar_url,u.status " +
                "from user u " +
                "inner join friend_relation fr on u.id = fr.user_id " +
                "where u.id=? and fr.status=1";
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
                "where receiver_id=? ";
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
}
