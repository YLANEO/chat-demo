package com.ylaneo.chat_client_android.manager;

import com.ylaneo.chat_client_android.model.ChatMessage;
import com.ylaneo.chat_client_android.network.ProtocolUtil;
import com.ylaneo.chat_client_android.network.TcpClient;
import com.ylaneo.chat_client_android.util.DateTimeUtils;
import com.ylaneo.chat_client_android.util.EntityParser;

import java.io.IOException;
import java.util.List;

/**
 * 消息相关业务封装（对应桌面端 MessageFacadeImpl）
 */
public class MessageManager {

    private static volatile MessageManager instance;

    private MessageManager() {
    }

    public static MessageManager getInstance() {
        if (instance == null) {
            synchronized (MessageManager.class) {
                if (instance == null) {
                    instance = new MessageManager();
                }
            }
        }
        return instance;
    }

    /**
     * 获取两个用户之间的所有消息
     */
    public List<ChatMessage> getAllMessages(int userId, int friendId) {
        String request = "<getAllMessage>" + "<" + userId + ">" + "<" + friendId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            EntityParser.WrapperResult wr = EntityParser.parseWrapper(response);
            if (!wr.isSuccess()) {
                throw new RuntimeException(wr.message);
            }
            // wr.dataFields 第一项是 count，跳过
            if (wr.dataFields.isEmpty()) {
                return new java.util.ArrayList<>();
            }
            List<String> dataFields = wr.dataFields.subList(1, wr.dataFields.size());
            return EntityParser.parseMessages(dataFields);
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 发送消息
     *
     * @return 服务端保存后的 ChatMessage（包含 chatMessageId 和时间戳）
     */
    public ChatMessage sendMessage(int senderId, int receiverId, int type, String content, int status) {
        String request = "<sendMessage>" + "<" + senderId + ">" + "<" + receiverId + ">" + "<" + type + ">" + "<" + content + ">" + "<" + status + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess()) {
                throw new RuntimeException(pr.message);
            }
            // 响应字段：msgId senderId receiverId type content readAt status createTime updateTime
            // 对应 pr.data.get(0)..(7)
            if (pr.data.size() < 8) {
                throw new RuntimeException("发送消息响应字段不完整");
            }
            ChatMessage m = new ChatMessage();
            try {
                m.setChatMessageId(Integer.parseInt(pr.data.get(0)));
                m.setSenderId(Integer.parseInt(pr.data.get(1)));
                m.setReceiverId(Integer.parseInt(pr.data.get(2)));
                m.setType(Integer.parseInt(pr.data.get(3)));
            } catch (NumberFormatException ignore) {
            }
            m.setContent(pr.data.get(4));
            m.setReadAt(DateTimeUtils.parse(pr.data.get(5)));
            try {
                m.setStatus(Integer.parseInt(pr.data.get(6)));
            } catch (NumberFormatException ignore) {
            }
            m.setCreateTime(DateTimeUtils.parse(pr.data.get(7)));
            m.setUpdateTime(DateTimeUtils.parse(pr.data.size() > 8 ? pr.data.get(8) : null));
            return m;
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }

    /**
     * 根据消息 ID 查找单条消息
     */
    public ChatMessage findMessageById(int messageId) {
        String request = "<findMessageById>" + "<" + messageId + ">";
        try {
            String response = TcpClient.getInstance().sendRequest(request);
            ProtocolUtil.ParsedResponse pr = ProtocolUtil.parseResponse(response);
            if (!pr.isSuccess() || pr.data.size() < 8) {
                return null;
            }
            ChatMessage m = new ChatMessage();
            try {
                m.setChatMessageId(Integer.parseInt(pr.data.get(0)));
                m.setSenderId(Integer.parseInt(pr.data.get(1)));
                m.setReceiverId(Integer.parseInt(pr.data.get(2)));
                m.setType(Integer.parseInt(pr.data.get(3)));
                m.setStatus(Integer.parseInt(pr.data.get(6)));
            } catch (NumberFormatException ignore) {
            }
            m.setContent(pr.data.get(4));
            m.setReadAt(DateTimeUtils.parse(pr.data.get(5)));
            m.setCreateTime(DateTimeUtils.parse(pr.data.get(7)));
            return m;
        } catch (IOException e) {
            throw new RuntimeException("网络异常: " + e.getMessage(), e);
        }
    }
}
