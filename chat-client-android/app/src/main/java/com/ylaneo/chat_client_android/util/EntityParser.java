package com.ylaneo.chat_client_android.util;

import com.ylaneo.chat_client_android.model.ChatMessage;
import com.ylaneo.chat_client_android.model.FriendRequestDto;
import com.ylaneo.chat_client_android.model.User;
import com.ylaneo.chat_client_android.network.ProtocolUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 实体解析工具：把协议字段列表转成实体
 *
 * 协议格式（与桌面端一致）：
 *   响应：<0><成功><count><item1_field1><item1_field2>...
 *
 * 字段数说明：
 *   - User 列表：每条 5 个字段（userid username nickname avatar status）
 *   - ChatMessage 列表：每条 9 个字段（msgId senderId receiverId type content readAt status createTime updateTime）
 *   - FriendRequestDto 列表：每条 8 个字段（requestId senderId senderUsername senderNickname senderAvatar message status createTime）
 */
public class EntityParser {

    private EntityParser() {
    }

    /**
     * 解析响应为 Result 对象（包含 code/message/data）
     * data 部分是裸字段列表（去掉 count）
     */
    public static class WrapperResult {
        public final int code;
        public final String message;
        public final List<String> dataFields; // 去掉 count 后的所有字段

        public WrapperResult(int code, String message, List<String> dataFields) {
            this.code = code;
            this.message = message;
            this.dataFields = dataFields;
        }

        public boolean isSuccess() {
            return code == 0;
        }
    }

    public static WrapperResult parseWrapper(String response) {
        ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
        List<String> data = new ArrayList<>(pr.data);
        // data.get(0) = count，后面的都是业务数据
        return new WrapperResult(pr.code, pr.message, data);
    }

    /**
     * 解析消息列表（每条 9 个字段）
     *
     * @param dataFields 去掉 count 后的字段列表
     */
    public static List<ChatMessage> parseMessages(List<String> dataFields) {
        List<ChatMessage> result = new ArrayList<>();
        int i = 0;
        while (i + 9 <= dataFields.size()) {
            try {
                ChatMessage m = new ChatMessage();
                m.setChatMessageId(parseInt(dataFields.get(i)));
                m.setSenderId(parseInt(dataFields.get(i + 1)));
                m.setReceiverId(parseInt(dataFields.get(i + 2)));
                m.setType(parseInt(dataFields.get(i + 3)));
                m.setContent(dataFields.get(i + 4));
                m.setReadAt(DateTimeUtils.parse(dataFields.get(i + 5)));
                m.setStatus(parseInt(dataFields.get(i + 6)));
                m.setCreateTime(DateTimeUtils.parse(dataFields.get(i + 7)));
                m.setUpdateTime(DateTimeUtils.parse(dataFields.get(i + 8)));
                result.add(m);
            } catch (Exception ignore) {
            }
            i += 9;
        }
        return result;
    }

    /**
     * 解析用户列表（每条 5 个字段）
     */
    public static List<User> parseUsers(List<String> dataFields) {
        List<User> result = new ArrayList<>();
        int i = 0;
        while (i + 5 <= dataFields.size()) {
            try {
                User u = new User();
                u.setUserid(parseInt(dataFields.get(i)));
                u.setUsername(dataFields.get(i + 1));
                u.setNickname(dataFields.get(i + 2));
                u.setAvatarUrl(dataFields.get(i + 3));
                u.setStatus(parseInt(dataFields.get(i + 4)));
                result.add(u);
            } catch (Exception ignore) {
            }
            i += 5;
        }
        return result;
    }

    /**
     * 解析好友申请列表（每条 8 个字段）
     */
    public static List<FriendRequestDto> parseFriendRequests(List<String> dataFields) {
        List<FriendRequestDto> result = new ArrayList<>();
        int i = 0;
        while (i + 8 <= dataFields.size()) {
            try {
                FriendRequestDto r = new FriendRequestDto();
                r.setRequestId(parseInt(dataFields.get(i)));
                r.setSenderId(parseInt(dataFields.get(i + 1)));
                r.setSenderUsername(dataFields.get(i + 2));
                r.setSenderNickname(dataFields.get(i + 3));
                r.setSenderAvatar(dataFields.get(i + 4));
                r.setMessage(dataFields.get(i + 5));
                r.setStatus(parseInt(dataFields.get(i + 6)));
                r.setCreateTime(DateTimeUtils.parse(dataFields.get(i + 7)));
                result.add(r);
            } catch (Exception ignore) {
            }
            i += 8;
        }
        return result;
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
