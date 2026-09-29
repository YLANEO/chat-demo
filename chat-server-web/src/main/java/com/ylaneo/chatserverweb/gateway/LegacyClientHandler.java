package com.ylaneo.chatserverweb.gateway;

import com.ylaneo.chatserverweb.dto.RegisterDto;
import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.FriendRequest;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import com.ylaneo.chatserverweb.service.ContactService;
import com.ylaneo.chatserverweb.service.MessageService;
import com.ylaneo.chatserverweb.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/*从桌面端的ServerThread演变而来，
变化：
1.不再new各种service而是通过Spring框架注入各个service
2.不再调用"自己写的"UserService（旧版签名吃整条尖括号request）改成调web版的service（签名是已经拆好的参数）
3. 不再自己用 if-else 拼尖括号字符串：调 LegacyResponseBuilder
4. 推送部分（ContactServiceImpl.handleRequest 中原本写Server.pushStatusUpdate(senderId, "<push><friendListUpdate>...")这一句）
保留在 ServiceImpl 里，不在这里做。

协议对应原ServerThread.dispatch
短连接流程：读一次、处理、写响应、关socket
<login>：调用UserService.login
长连接（注册推送 socket，不关连接，由 HeartbeatHandler 接管）：
<registerPushSocket>   -> LegacyPushNotifier.registerPush + 启动 HeartbeatHandler*/

@Component
public class LegacyClientHandler implements Runnable {

    @Autowired
    private UserService userService;

    @Autowired
    private ContactService contactService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private LegacyRequestParser parser;

    @Autowired
    private LegacyResponseBuilder responseBuilder;

    @Autowired
    private LegacyPushNotifier pushNotifier;

    private final Socket socket;

    public LegacyClientHandler() {
        this.socket = null; // Spring 用无参构造
    }

    //另一种构造
    public LegacyClientHandler(Socket socket) {
        this.socket = socket;
    }

    /*给LegacyTcpServer调用，把Spring注入的字段塞进去。
    因为LegacyClientHandler是在LegacyTcpServer里new出来的，
    走的是构造注入，没法直接拿@Autowired字段。*/
    public LegacyClientHandler(Socket socket, UserService userService, ContactService contactService,
                               MessageService messageService, LegacyRequestParser parser, LegacyResponseBuilder responseBuilder,
                               LegacyPushNotifier pushNotifier) {
        this.socket = socket;
        this.userService = userService;
        this.contactService = contactService;
        this.messageService = messageService;
        this.parser = parser;
        this.responseBuilder = responseBuilder;
        this.pushNotifier = pushNotifier;
    }

    //这个Runnable被执行时，自动执行这个函数
    @Override
    public void run() {
        try {
            //调用对应的接口
            dispatch();
        } catch (IOException e) {
            System.out.println("[LegacyClient] IO 异常: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /*主入口：读 socket 一次性请求 → 解析 → 路由 → 写响应。
    原 chat-server/ServerThread.dispatch() 完整翻译。*/
    private void dispatch() throws IOException {
        //读请求内容
        byte[] buffer = new byte[65535];
        InputStream is = socket.getInputStream();
        int len = is.read(buffer);
        if (len <= 0) {
            return;
        }

        String request = new String(buffer, 0, len);
        System.out.println("[LegacyClient] 收到原始请求长度=" + len + ", 内容=" + request);
        //调用route方法处理收到的请求（可能是长连接请求，也可能是短连接请求）
        String response = route(request);
        boolean keepAlive = (response == null);//如果response返回值是null，则需要让这个socket连接保持连接不断开。也就是变成一个长连接

        //这里判断是不是短连接的分支，如果是才需要使用os对象进行数据的写入
        OutputStream os = socket.getOutputStream();
        if (!keepAlive && response != null && !response.isEmpty()) {
            os.write(response.getBytes());
            os.flush();
        }

        //短连接：写完即关。除非是<registerPushSocket>长连接分支（response==null）
        //socket由HeartbeatHandler和PushListener共同维护，绝不能在这里close
        if (!keepAlive) {
            socket.close();
            System.out.println("[LegacyClient] 短连接请求socket 已关闭");
        } else {
            System.out.println("[LegacyClient] 长连接已交接，socket 保持打开");
        }
    }

    /**
     * 路由：根据请求前缀决定调哪个service。
     * 路径上与ServerThread.dispatch完全对应。
     */
    private String route(String request) {
        //根据返回的这个response是否为空来判断是否需要断开这个socket连接（response为null的时候不断开，转为长连接）。
        String response;
        try {
            //UserServiceImpl对应的一组
            if (request.startsWith("<login>")) {
                List<String> p = parser.handleParams(request);
                String username = p.get(1);
                String password = p.get(2);
                // 改成Result<User>，让buildLogin走4字段分支，桌面端才能拿到userId
                Result<User> r = userService.login(username, password);
                response = responseBuilder.buildLogin(r);
            } else if (request.startsWith("<register>")) {
                List<String> p = parser.handleParams(request);
                RegisterDto dto = new RegisterDto(p.get(1), p.get(2), p.get(3));
                Result<?> r = userService.register(dto);
                response = responseBuilder.buildRegister(r);
            } else if (request.startsWith("<updateUsername>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                String reUsername = p.get(2);
                Result<?> r = userService.updateUsername(userId, reUsername);
                response = responseBuilder.buildUpdateUsername(r);
            } else if (request.startsWith("<getUsername>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                Result<User> r = userService.getUsername(userId);
                response = responseBuilder.buildGetUsername(r);
            } else if (request.startsWith("<updateUserStatus>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                Integer userStatus = Integer.valueOf(p.get(2));
                Result<?> r = userService.updateUserStatus(userId, userStatus);
                response = responseBuilder.buildUpdateUserStatus(r);
            } else if (request.startsWith("<updateUserAvatar>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                String avatar = p.get(2);
                Result<?> r = userService.updateUserAvatar(userId, avatar);
                response = responseBuilder.buildUpdateUserAvatar(r);
            } else if (request.startsWith("<updateNickname>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                String newNickname = p.get(2);
                Result<?> r = userService.updateNickname(userId, newNickname);
                response = responseBuilder.buildUpdateNickname(r);
            } else if (request.startsWith("<getUserData>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                Result<User> r = userService.getUserData(userId);
                response = responseBuilder.buildGetUserData(r);
            }
            //ContactServiceImpl对应的一组
            else if (request.startsWith("<getFriendList>")) {
                List<String> p = parser.handleParams(request);
                Integer currentUserId = Integer.valueOf(p.get(1));
                Result<?> r = contactService.getFriendList(currentUserId);
                response = responseBuilder.buildGetFriendList(r);
            } else if (request.startsWith("<searchUserByKeyword>")) {
                List<String> p = parser.handleParams(request);
                String keyword = p.get(1);
                Integer currentUserId = Integer.valueOf(p.get(2));
                Result<?> r = contactService.searchUserByKeyword(keyword, currentUserId);
                response = responseBuilder.buildSearchUserByKeyword(r);
            } else if (request.startsWith("<sendFriendRequest>")) {
                List<String> p = parser.handleParams(request);
                Integer senderId = Integer.valueOf(p.get(1));
                Integer receiverId = Integer.valueOf(p.get(2));
                String message = p.get(3);
                Result<?> r = contactService.sendFriendRequest(senderId, receiverId, message);
                response = responseBuilder.buildSendFriendRequest(r);
            } else if (request.startsWith("<getFriendRequests>")) {
                List<String> p = parser.handleParams(request);
                Integer receiverId = Integer.valueOf(p.get(1));
                Result<?> r = contactService.getFriendRequests(receiverId);
                response = responseBuilder.buildGetFriendRequests(r);
            } else if (request.startsWith("<handleRequest>")) {
                List<String> p = parser.handleParams(request);
                Integer friendRequestId = Integer.valueOf(p.get(1));
                Integer senderId = Integer.valueOf(p.get(2));
                Integer receiverId = Integer.valueOf(p.get(3));
                Integer status = Integer.valueOf(p.get(4));
                Result<?> r = contactService.handleRequest(friendRequestId, senderId, receiverId, status);
                response = responseBuilder.buildHandleRequest(r);
            } else if (request.startsWith("<isFriend>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                Integer friendId = Integer.valueOf(p.get(2));
                Result<Integer> r = contactService.isFriend(userId, friendId);
                response = responseBuilder.buildIsFriend(r);
            } else if (request.startsWith("<hasPendingRequest>")) {
                List<String> p = parser.handleParams(request);
                Integer senderId = Integer.valueOf(p.get(1));
                Integer receiverId = Integer.valueOf(p.get(2));
                Result<Boolean> r = contactService.hasPendingRequest(senderId, receiverId);
                response = responseBuilder.buildHasPendingRequest(r);
            } else if (request.startsWith("<getFriendIds>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                Result<?> r = contactService.getFriendIds(userId);
                response = responseBuilder.buildGetFriendIds(r);
            }
            //MessageServiceImpl对应的一组
            else if (request.startsWith("<getAllMessage>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));
                Integer friendId = Integer.valueOf(p.get(2));
                Result<?> r = messageService.getAllMessage(userId, friendId);
                response = responseBuilder.buildGetAllMessage(r);
            } else if (request.startsWith("<sendMessage>")) {
                List<String> p = parser.handleParams(request);
                Integer senderId = Integer.valueOf(p.get(1));
                Integer receiverId = Integer.valueOf(p.get(2));
                Integer type = Integer.valueOf(p.get(3));
                String content = p.get(4);
                Integer status = Integer.valueOf(p.get(5));
                Result<ChatMessage> r = messageService.sendMessage(senderId, receiverId, type, content, status);
                response = responseBuilder.buildSendMessage(r);
            } else if (request.startsWith("<findMessageById>")) {
                List<String> p = parser.handleParams(request);
                Integer messageId = Integer.valueOf(p.get(1));
                Result<ChatMessage> r = messageService.findMessageById(messageId);
                response = responseBuilder.buildFindMessageById(r);
            }
            //长连接分支：<registerPushSocket>，走这条分支的话，这个socket连接不会断开。
            else if (request.startsWith("<registerPushSocket>")) {
                List<String> p = parser.handleParams(request);
                Integer userId = Integer.valueOf(p.get(1));

                //与原ServerThread基本行为一致：
                //1.注册到pushNotifier
                //2.启动独立心跳线程
                //3.不返回响应，让连接保持打开
                //上线通知：通知所有好友该用户已上线
                userService.markUserOnlineAndNotify(userId);

                // 注册到pushNotifier并启动心跳线程
                pushNotifier.registerPush(userId, socket);
                new HeartbeatHandler(socket, userId, pushNotifier).start();
                return null;  // dispatch() 检测到 response 为 null 不会写响应
            }
            // 其它：保持与原ServerThread一样的"未知命令"响应
            else {
                String prefix = request.substring(0, Math.min(20, request.length()));
                response = "<1><未知命令: " + prefix + ">";
            }
        } catch (Exception e) {
            System.out.println("[LegacyClient] 处理异常: " + e.getMessage());
            e.printStackTrace();
            response = "<1><服务器内部错误>";
        }

        return response;
    }
}
