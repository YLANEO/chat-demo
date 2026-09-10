package com.ylaneo.chatserverweb.gateway;

import com.ylaneo.chatserverweb.dto.FriendRequestDto;
import com.ylaneo.chatserverweb.entity.ChatMessage;
import com.ylaneo.chatserverweb.entity.Result;
import com.ylaneo.chatserverweb.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 把Service层返回的Result对象翻译成Swing客户端识别的尖括号字符串。
 *
 * 来源（迁移说明）：
 *   这些方法是对 chat-server 项目中各 ServiceImpl 里
 *   response = "<" + ... + ">" 这种拼装代码的集中抽取。
 *
 *   chat-server/src/main/java/com/ylaneo/chatserver/service/impl中的各个接口的返回数据的尖括号拼装。
 *
 *   注意：所有response拼装规则严格按原代码1:1翻译，
 *         包括null字段转空串的逻辑（见MessageServiceImpl中的直接传null的处理
 *         会导致客户端接收到字符串\"null\"，而非真正的空字符串"）。
 *
 *   每个方法对应一个具体命令，便于LegacyClientHandler调用。
 */
@Component
public class LegacyResponseBuilder {

    // ============================================================
    // 通用工具
    // ============================================================

    /**
     * 给一个值外面包尖括号，并处理null转换为""
     * 对应原代码中各处response = response + "<" + value + ">" 的拼接。
     */
    private static String angle(Object value) {
        if (value == null) {
            return "<>";
        }
        return "<" + value + ">";
    }

    /**
     * 头部通用：<code><message>
     */
    private static String head(Result<?> r) {
        return angle(r.getCode()) + angle(r.getMessage());
    }

    /**
     * 原代码中所有成功路径都会在头部返回<code><message>
     * 失败路径也返回<code><message>。这里统一生成。
     */
    private static String simple(Result<?> r) {
        return head(r);
    }

    // ============================================================
    // UserServiceImpl 相关
    // ============================================================

    /**
     * 对应<login>命令。
     * 成功：<code><message><userid><username>
     * 失败：<code><message>
     *
     * 原代码：UserServiceImpl.login()
     */
    public String buildLogin(Result<?> r) {
        if (r.getCode() == 0 && r.getData() instanceof User) {
            User user = (User) r.getData();
            return angle(r.getCode()) + angle(r.getMessage())
                    + angle(user.getUserid()) + angle(user.getUsername());
        }
        return simple(r);
    }

    /**
     * 对应 <register> 命令。
     * 成功/失败统一格式：<code><message>
     *
     * 原代码：UserServiceImpl.register()
     */
    public String buildRegister(Result<?> r) {
        return simple(r);
    }

    /**
     * 对应<updateUsername>命令。
     *
     * 原代码：UserServiceImpl.updateUsername()
     */
    public String buildUpdateUsername(Result<?> r) {
        return simple(r);
    }

    /**
     * 对应<getUsername>命令。
     * 成功：<code><message><userid><username>
     * 失败：<code><message>
     *
     * 原代码：UserServiceImpl.getUsername()
     */
    public String buildGetUsername(Result<?> r) {
        if (r.getCode() == 0 && r.getData() instanceof User) {
            User user = (User) r.getData();
            return angle(r.getCode()) + angle(r.getMessage())
                    + angle(user.getUserid()) + angle(user.getUsername());
        }
        return simple(r);
    }

    /**
     * 对应<updateUserStatus>命令。
     *
     * 原代码：UserServiceImpl.updateUserStatus()
     */
    public String buildUpdateUserStatus(Result<?> r) {
        return simple(r);
    }

    /**
     * 对应<updateUserAvatar>命令。
     *
     * 原代码：UserServiceImpl.updateUserAvatar()
     */
    public String buildUpdateUserAvatar(Result<?> r) {
        return simple(r);
    }

    /**
     * 对应<updateNickname>命令。
     *
     * 原代码：UserServiceImpl.updateNickname()
     */
    public String buildUpdateNickname(Result<?> r) {
        return simple(r);
    }

    /**
     * 对应<getUserData>命令。
     * 成功：<code><message><username><nickname><avatar_url><status>
     * 失败：<code><message>
     *
     * 原代码：UserServiceImpl.getUserData()
     */
    public String buildGetUserData(Result<?> r) {
        if (r.getCode() == 0 && r.getData() instanceof User) {
            User user = (User) r.getData();
            return angle(r.getCode()) + angle(r.getMessage())
                    + angle(user.getUsername())
                    + angle(user.getNickname())
                    + angle(user.getAvatar_url())
                    + angle(user.getStatus());
        }
        return simple(r);
    }

    // ============================================================
    // ContactServiceImpl 相关
    // ============================================================

    /**
     * 对应<getFriendList>命令。
     * 成功：<code><message><count>
     *       <id1><username1><nickname1><avatar_url1><status1>
     *       <id2>...
     * 失败：<code><message>
     *
     * 原代码：ContactServiceImpl.getFriendList()
     */
    public String buildGetFriendList(Result<?> r) {
        StringBuilder sb = new StringBuilder();
        if (r.getCode() == 0 && r.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<User> friendList = (List<User>) r.getData();
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
            int countList = friendList.size();
            sb.append(angle(countList));
            for (int i = 0; i < countList; i++) {
                User u = friendList.get(i);
                sb.append(angle(u.getUserid()))
                        .append(angle(u.getUsername()))
                        .append(angle(u.getNickname()))
                        .append(angle(u.getAvatar_url()))
                        .append(angle(u.getStatus()));
            }
        } else {
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
        }
        return sb.toString();
    }

    /**
     * 对应<searchUserByKeyword>命令。
     * 同getFriendList格式。
     *
     * 原代码：ContactServiceImpl.searchUserByKeyword()
     */
    public String buildSearchUserByKeyword(Result<?> r) {
        //与getFriendList格式完全一致
        return buildGetFriendList(r);
    }

    /**
     * 对应<sendFriendRequest>命令。
     *
     * 原代码：ContactServiceImpl.sendFriendRequest()
     */
    public String buildSendFriendRequest(Result<?> r) {
        return simple(r);
    }

    /**
     * 对应<getFriendRequests>命令。
     * 成功：<code><message><count>
     *       <requestId><senderId><senderUsername><senderNickname>
     *       <senderAvatar><message><status><createTime>
     * 失败：<code><message>
     *
     * 原代码：ContactServiceImpl.getFriendRequests()
     */
    public String buildGetFriendRequests(Result<?> r) {
        StringBuilder sb = new StringBuilder();
        if (r.getCode() == 0 && r.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<FriendRequestDto> friendRequestDtoList = (List<FriendRequestDto>) r.getData();
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
            int countList = friendRequestDtoList.size();
            sb.append(angle(countList));
            for (int i = 0; i < countList; i++) {
                FriendRequestDto fr = friendRequestDtoList.get(i);
                sb.append(angle(fr.getRequestId()))
                        .append(angle(fr.getSenderId()))
                        .append(angle(fr.getSenderUsername()))
                        .append(angle(fr.getSenderNickname()))
                        .append(angle(fr.getSenderAvatar()))
                        .append(angle(fr.getMessage()))
                        .append(angle(fr.getStatus()))
                        .append(angle(fr.getCreateTime()));//null时angle转""
            }
        } else {
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
        }
        return sb.toString();
    }

    /**
     * 对应<handleRequest>命令。
     *
     * 原代码：ContactServiceImpl.handleRequest()
     * 业务侧的状态推送（同意时互推 friendListUpdate）由
     * LegacyPushNotifier/ContactServiceImpl内部协调，
     * 这里只负责返回响应字符串。
     */
    public String buildHandleRequest(Result<?> r) {
        return simple(r);
    }

    /**
     * 对应<isFriend>命令。
     * 成功/失败统一：<code><message><data>
     *
     * 原代码：ContactServiceImpl.isFriend()
     */
    public String buildIsFriend(Result<?> r) {
        return simple(r) + angle(r.getData());
    }

    /**
     * 对应<hasPendingRequest>命令。
     * 成功/失败统一：<code><message><data>
     *
     * 原代码：ContactServiceImpl.hasPendingRequest()
     */
    public String buildHasPendingRequest(Result<?> r) {
        return simple(r) + angle(r.getData());
    }

    /**
     * 对应<getFriendIds>命令。
     * 成功：<code><message><count><id1><id2>...
     * 失败：<code><message>
     *
     * 原代码：ContactServiceImpl.getFriendIds()
     */
    public String buildGetFriendIds(Result<?> r) {
        StringBuilder sb = new StringBuilder();
        if (r.getCode() == 0 && r.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<Integer> friendIdList = (List<Integer>) r.getData();
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
            int countList = friendIdList.size();
            sb.append(angle(countList));
            for (int i = 0; i < countList; i++) {
                sb.append(angle(friendIdList.get(i)));
            }
        } else {
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
        }
        return sb.toString();
    }

    // ============================================================
    // MessageServiceImpl相关
    // ============================================================

    /**
     * 对应<getAllMessage>命令。
     * 成功：<code><message><count>
     *       <chatMessageId><senderId><receiverId><type><content>
     *       <readAt><status><createTime><updateTime>//注意：null字段转""
     *       （循环每条消息）
     * 失败：<code><message>
     *
     * 原代码：MessageServiceImpl.getAllMessage()
     * 关键：每个时间字段都用angle()包一层，null 自动变 "<>"
     */
    public String buildGetAllMessage(Result<?> r) {
        StringBuilder sb = new StringBuilder();
        if (r.getCode() == 0 && r.getData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<ChatMessage> messageList = (List<ChatMessage>) r.getData();
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
            int countList = messageList.size();
            sb.append(angle(countList));
            for (int i = 0; i < countList; i++) {
                ChatMessage m = messageList.get(i);
                sb.append(angle(m.getChatMessageId()))
                        .append(angle(m.getSenderId()))
                        .append(angle(m.getReceiverId()))
                        .append(angle(m.getType()))
                        .append(angle(m.getContent()))
                        .append(angle(m.getReadAt()))//null->"<>"
                        .append(angle(m.getStatus()))
                        .append(angle(m.getCreateTime()))//null->"<>"
                        .append(angle(m.getUpdateTime()));//null->"<>"
            }
        } else {
            sb.append(angle(r.getCode())).append(angle(r.getMessage()));
        }
        return sb.toString();
    }

    /**
     * 对应<sendMessage>命令。
     * 成功：<code><message><chatMessageId><senderId><receiverId>
     *       <type><content><readAt><status><createTime><updateTime>
     * 失败：<code><message>
     *
     * 原代码：MessageServiceImpl.sendMessage()
     * 注意：推送<push><messageAdd>由MessageServiceImpl内部做，
     *       这里只负责响应字符串。
     */
    public String buildSendMessage(Result<?> r) {
        if (r.getCode() == 0 && r.getData() instanceof ChatMessage) {
            ChatMessage m = (ChatMessage) r.getData();
            StringBuilder sb = new StringBuilder();
            sb.append(angle(r.getCode())).append(angle(r.getMessage()))
                    .append(angle(m.getChatMessageId()))
                    .append(angle(m.getSenderId()))
                    .append(angle(m.getReceiverId()))
                    .append(angle(m.getType()))
                    .append(angle(m.getContent()))
                    .append(angle(m.getReadAt()))//null->"<>"
                    .append(angle(m.getStatus()))
                    .append(angle(m.getCreateTime()))//null->"<>"
                    .append(angle(m.getUpdateTime()));//null->"<>"
            return sb.toString();
        }
        return simple(r);
    }

    /**
     * 对应<findMessageById>命令。同sendMessage数据格式。
     *
     * 原代码：MessageServiceImpl.findMessageById()
     */
    public String buildFindMessageById(Result<?> r) {
        //与buildSendMessage格式完全一致
        return buildSendMessage(r);
    }

    // ============================================================
    // 推送消息构造（而不是响应）
    // ============================================================

    /**
     * 构造<push><messageAdd><senderId><messageId>推送字符串。
     *
     * 原代码：MessageServiceImpl.sendMessage()
     */
    public String buildPushMessageAdd(Integer senderId, Integer messageId) {
        return "<push><messageAdd>" + angle(senderId) + angle(messageId);
    }

    /**
     * 构造<push><statusUpdate><userId><status>推送字符串。
     *
     * 原代码：UserServiceImpl.broadcastStatusUpdate()
     */
    public String buildPushStatusUpdate(Integer userId, Integer status) {
        return "<push><statusUpdate>" + angle(userId) + angle(status);
    }

    /**
     * 构造<push><friendListUpdate><friendId>推送字符串。
     *
     * 原代码：ContactServiceImpl.handleRequest()
     */
    public String buildPushFriendListUpdate(Integer friendId) {
        return "<push><friendListUpdate>" + angle(friendId);
    }
}
