package com.ylaneo.chatserverweb.service.impl;

import com.ylaneo.chatserverweb.dao.ContactDao;
import com.ylaneo.chatserverweb.dao.UserDao;
import com.ylaneo.chatserverweb.dto.RegisterDto;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.service.UserService;
import com.ylaneo.chatserverweb.util.JwtUtil;
import com.ylaneo.chatserverweb.websocket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service//交给Spring管理
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final ContactDao contactDao;
    private final ChatWebSocketHandler webSocketHandler;

    public UserServiceImpl(UserDao userDao, ContactDao contactDao, ChatWebSocketHandler webSocketHandler) {
        this.userDao = userDao;
        this.contactDao = contactDao;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public Result register(RegisterDto registerDto) {
        return userDao.register(registerDto);
    }

    @Override
    public Result<String> login(String username, String password) {
        Result<User> result= userDao.login(username, password);
        //
        if (result.getCode() != 0) {
            return Result.error(result.getMessage());//直接传DAO的错误信息
        }
        User loginUser = result.getData();
        if (loginUser == null) {
            return Result.error("用户不存在");
        }
        //添加jwt令牌
        Map<String,Object>claims=new HashMap<>();
        //由两部分构成，用户id和用户的username
        claims.put("userid",loginUser.getUserid());
        claims.put("username",loginUser.getUsername());
        String token= JwtUtil.genToken(claims);//使用token（一个string类型的字符串）接受生成的jwt令牌,令牌中包含了用户的id和用户名
        return Result.success(token);
    }

    @Override
    public Result updateUsername(Integer userId, String reUsername) {
        return userDao.updateUsername(userId, reUsername);
    }

    @Override
    public Result getUsername(Integer userId) {
        return userDao.getUsername(userId);
    }

    @Override
    public Result updateUserStatus(Integer userId, Integer userStatus) {
        Result result = userDao.updateUserStatus(userId, userStatus);

        // ★ 关键变化：这里不再调用Server.pushStatusUpdate()
        // 而是调用ChatWebSocketHandler.pushToUsers()推送WebSocket消息
        if (result.getCode() == 0) {
            broadcastStatusUpdate(userId, userStatus);
        }

        return result;
    }

    @Override
    public Result updateUserAvatar(Integer userId, String userAvatarUrl) {
        return userDao.updateUserAvatar(userId, userAvatarUrl);
    }

    @Override
    public Result updateNickname(Integer userId, String newNickname) {
        return userDao.updateNickname(userId, newNickname);
    }

    @Override
    public Result<User> getUserData(Integer userId) {
        return userDao.getUserData(userId);
    }

    /**
     * ★ 广播状态更新给所有好友
     * 原来: Server.pushStatusUpdate(friendId, "<push><statusUpdate>...")
     * 现在: webSocketHandler.pushToUsers(friendIds, pushMessage)
     */
    private void broadcastStatusUpdate(Integer userId, Integer status) {
        // 获取该用户的所有好友 ID
        Result<List<Integer>> friendIdsResult = contactDao.getFriendIds(userId);
        if (friendIdsResult.getCode() != 0 || friendIdsResult.getData() == null) {
            return;
        }

        // 构造推送消息
        Map<String, Object> pushMessage = Map.of(
                "type", "statusUpdate",
                "userId", userId,
                "status", status
        );

        // 推送给所有好友
        webSocketHandler.pushToUsers(friendIdsResult.getData(), pushMessage);
        System.out.println("状态更新已广播: userId=" + userId + ", status=" + status);
    }

    /**
     * 用户离线时调用（Controller 或 WebSocketHandler 断开连接时调用）
     * 原来: Server.userDisconnected(userId) → markUserOfflineAndNotify()
     */
    public void markUserOfflineAndNotify(Integer userId) {
        Result result = userDao.updateUserStatus(userId, 0);
        if (result.getCode() == 0) {
            broadcastStatusUpdate(userId, 0);
        }
    }
}