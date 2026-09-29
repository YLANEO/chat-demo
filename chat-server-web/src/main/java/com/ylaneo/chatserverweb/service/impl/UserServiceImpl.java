package com.ylaneo.chatserverweb.service.impl;

import com.ylaneo.chatserverweb.dao.ContactDao;
import com.ylaneo.chatserverweb.dao.UserDao;
import com.ylaneo.chatserverweb.dto.RegisterDto;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.gateway.LegacyPushNotifier;
import com.ylaneo.chatserverweb.gateway.LegacyResponseBuilder;
import com.ylaneo.chatserverweb.service.UserService;
import com.ylaneo.chatserverweb.util.JwtUtil;
import com.ylaneo.chatserverweb.websocket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService, ApplicationContextAware {
    private ApplicationContext applicationContext;

    private final UserDao userDao;
    private final ContactDao contactDao;
//    private final LegacyPushNotifier pushNotifier;
//    private final LegacyResponseBuilder responseBuilder;

    public UserServiceImpl(UserDao userDao, ContactDao contactDao) {
        this.userDao = userDao;
        this.contactDao = contactDao;
    }

    @Override
    public Result register(RegisterDto registerDto) {
        return userDao.register(registerDto);
    }

    @Override
    public Result<User> login(String username, String password) {
        Result<User> result= userDao.login(username, password);
        //
        if (result.getCode() != 0) {
            return Result.error(result.getMessage());//直接传DAO的错误信息
        }
        User loginUser = result.getData();
        if (loginUser == null) {
            return Result.error("用户不存在");
        }
        // 仍然生成 jwt token（web 端 / app 端可能要用到）
        Map<String,Object> claims = new HashMap<>();
        claims.put("userid", loginUser.getUserid());
        claims.put("username", loginUser.getUsername());
        String token = JwtUtil.genToken(claims);
        // 把 token 挂在 User 上一起返回，方便后续 web 端需要时直接拿
        loginUser.setToken(token);
        System.out.println("[UserServiceImpl] 登录成功: userId=" + loginUser.getUserid());
        // 返回完整的 User 对象，让 LegacyResponseBuilder.buildLogin() 能走 4 字段分支
        return Result.success(loginUser);
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

        //关键：这里不再调用Server.pushStatusUpdate()，而是调用ChatWebSocketHandler.pushToUsers()推送WebSocket消息
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
     * 广播状态更新给所有好友
     * 原来:Server.pushStatusUpdate(friendId,"<push><statusUpdate>...")
     * 现在:webSocketHandler.pushToUsers(friendIds,pushMessage)
     */
    private void broadcastStatusUpdate(Integer userId, Integer status) {
        //漏掉了这行！这是变量定义
        Result<List<Integer>> friendIdsResult = contactDao.getFriendIds(userId);
        if (friendIdsResult.getCode() != 0 || friendIdsResult.getData() == null) {
            return;
        }
        // web 端推送（JSON）
        Map<String, Object> webMessage = LegacyPushNotifier.buildWebStatusUpdate(userId, status);
        System.out.println("后端构造的map类型的 状态更新数据："+webMessage);
        ChatWebSocketHandler webSocketHandler = applicationContext.getBean(ChatWebSocketHandler.class);
        webSocketHandler.pushToUsers(friendIdsResult.getData(), webMessage);
        // swing 端推送（尖括号字符串）—— 随用随取
        LegacyResponseBuilder responseBuilder = applicationContext.getBean(LegacyResponseBuilder.class);
        LegacyPushNotifier pushNotifier = applicationContext.getBean(LegacyPushNotifier.class);
        String legacyMessage = responseBuilder.buildPushStatusUpdate(userId, status);
        pushNotifier.pushToUsers(friendIdsResult.getData(), legacyMessage);
        System.out.println("状态更新已广播: userId=" + userId + ", status=" + status);
    }

    /**
     * 用户上线时调用（LegacyClientHandler和ChatWebSocketHandler连接建立时调用）
     * 对称于markUserOfflineAndNotify
     */
    public void markUserOnlineAndNotify(Integer userId) {
        Result result = userDao.updateUserStatus(userId, 1);
        if (result.getCode() == 0) {
            //这里会先查询好友列表，再广播推送状态更新给所有好友
            broadcastStatusUpdate(userId, 1);
        }
    }

    /**
     * 用户离线时调用（Controller或WebSocketHandler断开连接时调用）
     * 原来:Server.userDisconnected(userId)→markUserOfflineAndNotify()
     */
    public void markUserOfflineAndNotify(Integer userId) {
        Result result = userDao.updateUserStatus(userId, 0);
        if (result.getCode() == 0) {
            broadcastStatusUpdate(userId, 0);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}